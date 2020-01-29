/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.mapValueParameters
import org.jetbrains.kotlin.ir.expressions.putTypeArguments
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegationResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

class ClassGenerator(
    declarationGenerator: DeclarationGenerator
) : DeclarationGeneratorExtension(declarationGenerator) {

    companion object {
        private val DESCRIPTOR_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            includePropertyConstant = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            verbose = true
            modifiers = DescriptorRendererModifier.ALL
        }

        fun <T : DeclarationDescriptor> List<T>.sortedByRenderer(): List<T> {
            val rendered = map(DESCRIPTOR_RENDERER::render)
            val sortedIndices = (0 until size).sortedWith(Comparator { i, j -> rendered[i].compareTo(rendered[j]) })
            return sortedIndices.map { this[it] }
        }
    }

    fun generateClass(ktClassOrObject: KtPureClassOrObject): IrClass {
        val classDescriptor = ktClassOrObject.findClassDescriptor(this.context.bindingContext)
        val startOffset = ktClassOrObject.getStartOffsetOfClassDeclarationOrNull() ?: ktClassOrObject.pureStartOffset
        val endOffset = ktClassOrObject.pureEndOffset

        return context.symbolTable.declareClass(
            startOffset, endOffset, IrDeclarationOrigin.DEFINED, classDescriptor,
            getEffectiveModality(ktClassOrObject, classDescriptor)
        ).buildWithScope { irClass ->
            declarationGenerator.generateGlobalTypeParametersDeclarations(irClass, classDescriptor.declaredTypeParameters)

            classDescriptor.typeConstructor.supertypes.mapTo(irClass.superTypes) {
                it.toIrType()
            }

            irClass.thisReceiver = context.symbolTable.declareValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                classDescriptor.thisAsReceiverParameter,
                classDescriptor.thisAsReceiverParameter.type.toIrType()
            )

            val irPrimaryConstructor = generatePrimaryConstructor(irClass, ktClassOrObject)
            if (irPrimaryConstructor != null) {
                generateDeclarationsForPrimaryConstructorParameters(irClass, irPrimaryConstructor, ktClassOrObject)
            }

            if (ktClassOrObject is KtClassOrObject) //todo: supertype list for synthetic declarations
                generateMembersDeclaredInSupertypeList(irClass, ktClassOrObject)

            generateMembersDeclaredInClassBody(irClass, ktClassOrObject)

            generateFakeOverrideMemberDeclarations(irClass, ktClassOrObject)

            if (irClass.isInline && ktClassOrObject is KtClassOrObject) {
                generateAdditionalMembersForInlineClasses(irClass, ktClassOrObject)
            }

            if (irClass.isData && ktClassOrObject is KtClassOrObject) {
                generateAdditionalMembersForDataClass(irClass, ktClassOrObject)
            }

            if (DescriptorUtils.isEnumClass(classDescriptor)) {
                generateAdditionalMembersForEnumClass(irClass)
            }
        }
    }

    private fun getEffectiveModality(ktClassOrObject: KtPureClassOrObject, classDescriptor: ClassDescriptor): Modality =
        when {
            !DescriptorUtils.isEnumClass(classDescriptor) ->
                classDescriptor.modality
            DescriptorUtils.hasAbstractMembers(classDescriptor) ->
                Modality.ABSTRACT
            ktClassOrObject.hasEnumEntriesWithClassMembers() ->
                Modality.OPEN
            else ->
                Modality.FINAL
        }

    private fun KtPureClassOrObject.hasEnumEntriesWithClassMembers(): Boolean {
        val body = this.body ?: return false
        return body.enumEntries.any { it.hasMemberDeclarations() }
    }

    private fun KtEnumEntry.hasMemberDeclarations() = declarations.isNotEmpty()

    private fun generateFakeOverrideMemberDeclarations(irClass: IrClass, ktClassOrObject: KtPureClassOrObject) {
        irClass.descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            .mapNotNull {
                it.safeAs<CallableMemberDescriptor>().takeIf { memberDescriptor ->
                    memberDescriptor?.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                }
            }
            .sortedByRenderer()
            .forEach { fakeOverride ->
                declarationGenerator.generateFakeOverrideDeclaration(fakeOverride, ktClassOrObject)?.let { irClass.declarations.add(it) }
            }
    }

    private fun generateMembersDeclaredInSupertypeList(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        val ktSuperTypeList = ktClassOrObject.getSuperTypeList() ?: return
        var delegateNumber = 0
        for (ktEntry in ktSuperTypeList.entries) {
            if (ktEntry is KtDelegatedSuperTypeEntry) {
                generateDelegatedImplementationMembers(irClass, ktEntry, delegateNumber++)
            }
        }
    }

    private fun generateDelegatedImplementationMembers(
        irClass: IrClass,
        ktEntry: KtDelegatedSuperTypeEntry,
        delegateNumber: Int
    ) {
        val ktDelegateExpression = ktEntry.delegateExpression!!
        val delegateType = getTypeInferredByFrontendOrFail(ktDelegateExpression)
        val superType = getOrFail(BindingContext.TYPE, ktEntry.typeReference!!)

        val superTypeConstructorDescriptor = superType.constructor.declarationDescriptor
        val superClass = superTypeConstructorDescriptor as? ClassDescriptor
            ?: throw AssertionError("Unexpected supertype constructor for delegation: $superTypeConstructorDescriptor")

        val propertyDescriptor = CodegenUtil.getDelegatePropertyIfAny(ktDelegateExpression, irClass.descriptor, context.bindingContext)

        val irDelegateField: IrField = if (CodegenUtil.isFinalPropertyWithBackingField(propertyDescriptor, context.bindingContext)) {
            irClass.properties.first { it.descriptor == propertyDescriptor }.backingField!!
        } else {
            val delegateDescriptor = IrImplementingDelegateDescriptorImpl(irClass.descriptor, delegateType, superType, delegateNumber)
            context.symbolTable.declareField(
                ktDelegateExpression.startOffsetSkippingComments, ktDelegateExpression.endOffset,
                IrDeclarationOrigin.DELEGATE,
                delegateDescriptor, delegateDescriptor.type.toIrType(),
                createBodyGenerator(irClass.symbol).generateExpressionBody(ktDelegateExpression)
            ).apply {
                irClass.addMember(this)
            }
        }

        val delegatesMap = DelegationResolver.getDelegates(irClass.descriptor, superClass, delegateType)
        val delegatedMembers = delegatesMap.keys.toList().sortedByRenderer()

        for (delegatedMember in delegatedMembers) {
            val overriddenMember = delegatedMember.overriddenDescriptors.find { it.containingDeclaration.original == superClass.original }
            if (overriddenMember != null) {
                val delegateToMember = delegatesMap[delegatedMember]
                    ?: throw AssertionError(
                        "No corresponding member in delegate type $delegateType for $delegatedMember overriding $overriddenMember"
                    )
                generateDelegatedMember(irClass, irDelegateField, delegatedMember, delegateToMember)
            }
        }
    }

    private fun generateDelegatedMember(
        irClass: IrClass,
        irDelegate: IrField,
        delegatedMember: CallableMemberDescriptor,
        delegateToMember: CallableMemberDescriptor
    ) {
        when (delegatedMember) {
            is FunctionDescriptor -> generateDelegatedFunction(irClass, irDelegate, delegatedMember, delegateToMember as FunctionDescriptor)
            is PropertyDescriptor -> generateDelegatedProperty(irClass, irDelegate, delegatedMember, delegateToMember as PropertyDescriptor)
        }
    }

    private fun generateDelegatedProperty(
        irClass: IrClass,
        irDelegate: IrField,
        delegatedDescriptor: PropertyDescriptor,
        delegateToDescriptor: PropertyDescriptor
    ) {
        irClass.addMember(generateDelegatedProperty(irDelegate, delegatedDescriptor, delegateToDescriptor))
    }

    private fun generateDelegatedProperty(
        irDelegate: IrField,
        delegatedDescriptor: PropertyDescriptor,
        delegateToDescriptor: PropertyDescriptor
    ): IrProperty {
        val startOffset = irDelegate.startOffset
        val endOffset = irDelegate.endOffset

        val irProperty = context.symbolTable.declareProperty(
            startOffset, endOffset, IrDeclarationOrigin.DELEGATED_MEMBER,
            delegatedDescriptor
        )

        irProperty.getter = generateDelegatedFunction(irDelegate, delegatedDescriptor.getter!!, delegateToDescriptor.getter!!)

        if (delegatedDescriptor.isVar) {
            irProperty.setter = generateDelegatedFunction(irDelegate, delegatedDescriptor.setter!!, delegateToDescriptor.setter!!)
        }
        return irProperty
    }

    private fun generateDelegatedFunction(
        irClass: IrClass,
        irDelegate: IrField,
        delegatedDescriptor: FunctionDescriptor,
        delegateToDescriptor: FunctionDescriptor
    ) {
        irClass.addMember(generateDelegatedFunction(irDelegate, delegatedDescriptor, delegateToDescriptor))
    }

    private fun generateDelegatedFunction(
        irDelegate: IrField,
        delegatedDescriptor: FunctionDescriptor,
        delegateToDescriptor: FunctionDescriptor
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            irDelegate.startOffset, irDelegate.endOffset,
            IrDeclarationOrigin.DELEGATED_MEMBER,
            delegatedDescriptor
        ).buildWithScope { irFunction ->
            FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)

            // TODO could possibly refer to scoped type parameters for property accessors
            irFunction.returnType = delegatedDescriptor.returnType!!.toIrType()

            irFunction.body = generateDelegateFunctionBody(irDelegate, delegatedDescriptor, delegateToDescriptor, irFunction)
        }

    private fun generateDelegateFunctionBody(
        irDelegate: IrField,
        delegatedDescriptor: FunctionDescriptor,
        delegateToDescriptor: FunctionDescriptor,
        irDelegatedFunction: IrSimpleFunction
    ): IrBlockBodyImpl {
        val startOffset = irDelegate.startOffset
        val endOffset = irDelegate.endOffset

        val irBlockBody = IrBlockBodyImpl(startOffset, endOffset)

        val substitutedDelegateTo = substituteDelegateToDescriptor(delegatedDescriptor, delegateToDescriptor)
        val returnType = substitutedDelegateTo.returnType!!

        val delegateToSymbol = context.symbolTable.referenceFunction(delegateToDescriptor.original)

        val irCall = IrCallImpl(
            startOffset, endOffset,
            returnType.toIrType(),
            delegateToSymbol,
            substitutedDelegateTo.typeParametersCount
        ).apply {
            context.callToSubstitutedDescriptorMap[this] = substitutedDelegateTo

            val typeArguments = getTypeArgumentsForOverriddenDescriptorDelegatingCall(delegatedDescriptor, delegateToDescriptor)
            putTypeArguments(typeArguments) { it.toIrType() }

            val dispatchReceiverParameter = irDelegatedFunction.dispatchReceiverParameter!!
            dispatchReceiver =
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irDelegate.symbol,
                    irDelegate.type,
                    IrGetValueImpl(
                        startOffset, endOffset,
                        dispatchReceiverParameter.type,
                        dispatchReceiverParameter.symbol
                    )
                )

            extensionReceiver =
                irDelegatedFunction.extensionReceiverParameter?.let { extensionReceiver ->
                    IrGetValueImpl(startOffset, endOffset, extensionReceiver.type, extensionReceiver.symbol)
                }

            mapValueParameters { overriddenValueParameter ->
                val delegatedValueParameter = delegatedDescriptor.valueParameters[overriddenValueParameter.index]
                val irDelegatedValueParameter = irDelegatedFunction.getIrValueParameter(delegatedValueParameter)
                IrGetValueImpl(startOffset, endOffset, irDelegatedValueParameter.type, irDelegatedValueParameter.symbol)
            }
        }

        if (KotlinBuiltIns.isUnit(returnType) || KotlinBuiltIns.isNothing(returnType)) {
            irBlockBody.statements.add(irCall)
        } else {
            val irReturn = IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, irDelegatedFunction.symbol, irCall)
            irBlockBody.statements.add(irReturn)
        }
        return irBlockBody
    }

    @Suppress("UNCHECKED_CAST")
    private fun <D : CallableMemberDescriptor> substituteDelegateToDescriptor(delegated: D, overridden: D): D =
        // PropertyAccessorDescriptor doesn't support 'substitute', so we substitute the corresponding property instead.
        when (overridden) {
            is PropertyGetterDescriptor -> substituteDelegateToDescriptor(
                (delegated as PropertyGetterDescriptor).correspondingProperty,
                overridden.correspondingProperty
            ).getter as D
            is PropertySetterDescriptor -> substituteDelegateToDescriptor(
                (delegated as PropertySetterDescriptor).correspondingProperty,
                overridden.correspondingProperty
            ).setter as D
            else -> {
                val delegatedTypeParameters = delegated.typeParameters
                val substitutor =
                    TypeSubstitutor.create(
                        overridden.typeParameters.associate {
                            val delegatedDefaultType = delegatedTypeParameters[it.index].defaultType
                            it.typeConstructor to TypeProjectionImpl(delegatedDefaultType)
                        }
                    )
                overridden.substitute(substitutor)!! as D
            }
        }

    private fun getTypeArgumentsForOverriddenDescriptorDelegatingCall(
        delegatedDescriptor: FunctionDescriptor,
        delegateToDescriptor: FunctionDescriptor
    ): Map<TypeParameterDescriptor, KotlinType>? {
        val keys = delegateToDescriptor.propertyIfAccessor.original.typeParameters
        if (keys.isEmpty()) return null

        val values = delegatedDescriptor.propertyIfAccessor.typeParameters

        val typeArguments = newHashMapWithExpectedSize<TypeParameterDescriptor, KotlinType>(keys.size)
        for ((i, overriddenTypeParameter) in keys.withIndex()) {
            typeArguments[overriddenTypeParameter] = values[i].defaultType
        }
        return typeArguments
    }

    private fun generateAdditionalMembersForInlineClasses(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(declarationGenerator).generateInlineClassMembers(ktClassOrObject, irClass)
    }

    private fun generateAdditionalMembersForDataClass(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(declarationGenerator).generateDataClassMembers(ktClassOrObject, irClass)
    }

    private fun generateAdditionalMembersForEnumClass(irClass: IrClass) {
        EnumClassMembersGenerator(declarationGenerator).generateSpecialMembers(irClass)
    }

    private fun generatePrimaryConstructor(irClass: IrClass, ktClassOrObject: KtPureClassOrObject): IrConstructor? {
        val classDescriptor = irClass.descriptor
        val primaryConstructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor ?: return null

        return FunctionGenerator(declarationGenerator)
            .generatePrimaryConstructor(primaryConstructorDescriptor, ktClassOrObject)
            .also {
                irClass.addMember(it)
            }
    }

    private fun generateDeclarationsForPrimaryConstructorParameters(
        irClass: IrClass,
        irPrimaryConstructor: IrConstructor,
        ktClassOrObject: KtPureClassOrObject
    ) {
        ktClassOrObject.primaryConstructor?.let { ktPrimaryConstructor ->
            irPrimaryConstructor.valueParameters.forEach {
                context.symbolTable.introduceValueParameter(it)
            }

            ktPrimaryConstructor.valueParameters.forEachIndexed { i, ktParameter ->
                val irValueParameter = irPrimaryConstructor.valueParameters[i]
                if (ktParameter.hasValOrVar()) {
                    val irProperty = PropertyGenerator(declarationGenerator)
                        .generatePropertyForPrimaryConstructorParameter(ktParameter, irValueParameter)
                    irClass.addMember(irProperty)
                }
            }
        }
    }

    private fun generateMembersDeclaredInClassBody(irClass: IrClass, ktClassOrObject: KtPureClassOrObject) {
        // generate real body declarations
        ktClassOrObject.body?.let { ktClassBody ->
            ktClassBody.declarations.mapNotNullTo(irClass.declarations) { ktDeclaration ->
                declarationGenerator.generateClassMemberDeclaration(ktDeclaration, irClass)
            }
        }

        // generate synthetic nested classes (including companion)
        irClass.descriptor
            .unsubstitutedMemberScope
            .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.ALL_NAME_FILTER)
            .asSequence()
            .filterIsInstance<SyntheticClassOrObjectDescriptor>()
            .mapTo(irClass.declarations) { declarationGenerator.generateSyntheticClassOrObject(it.syntheticDeclaration) }

        // synthetic functions and properties to classes must be contributed by corresponding lowering
    }

    fun generateEnumEntry(ktEnumEntry: KtEnumEntry): IrEnumEntry {
        val enumEntryDescriptor = getOrFail(BindingContext.CLASS, ktEnumEntry)

        // TODO this is a hack, pass declaration parent through generator chain instead
        val enumClassDescriptor = enumEntryDescriptor.containingDeclaration as ClassDescriptor
        val enumClassSymbol = context.symbolTable.referenceClass(enumClassDescriptor)
        val irEnumClass = enumClassSymbol.owner

        return context.symbolTable.declareEnumEntry(
            ktEnumEntry.startOffsetSkippingComments,
            ktEnumEntry.endOffset,
            IrDeclarationOrigin.DEFINED,
            enumEntryDescriptor
        ).buildWithScope { irEnumEntry ->
            irEnumEntry.parent = irEnumClass

            if (!enumEntryDescriptor.isExpect) {
                irEnumEntry.initializerExpression =
                    createBodyGenerator(irEnumEntry.symbol)
                        .generateEnumEntryInitializer(ktEnumEntry, enumEntryDescriptor)
            }

            if (ktEnumEntry.hasMemberDeclarations()) {
                irEnumEntry.correspondingClass = generateClass(ktEnumEntry)
            }
        }

    }
}
