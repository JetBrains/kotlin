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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.mapValueParameters
import org.jetbrains.kotlin.ir.expressions.putTypeArguments
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.util.StableDescriptorsComparator
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
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

    fun generateClass(ktClassOrObject: KtPureClassOrObject): IrClass {
        val classDescriptor = ktClassOrObject.findClassDescriptor(this.context.bindingContext)
        val startOffset = ktClassOrObject.pureStartOffset
        val endOffset = ktClassOrObject.pureEndOffset

        return context.symbolTable.declareClass(
            startOffset, endOffset, IrDeclarationOrigin.DEFINED, classDescriptor
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

    private fun generateFakeOverrideMemberDeclarations(irClass: IrClass, ktClassOrObject: KtPureClassOrObject) {
        irClass.descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            .mapNotNull {
                it.safeAs<CallableMemberDescriptor>().takeIf {
                    it?.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                }
            }
            .sortedWith(StableDescriptorsComparator)
            .forEach { fakeOverride ->
                irClass.addMember(declarationGenerator.generateFakeOverrideDeclaration(fakeOverride, ktClassOrObject))
            }
    }

    private fun generateMembersDeclaredInSupertypeList(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        val ktSuperTypeList = ktClassOrObject.getSuperTypeList() ?: return
        val delegatedMembers = irClass.descriptor.unsubstitutedMemberScope
            .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.DELEGATION }
            .sortedWith(StableDescriptorsComparator)
        if (delegatedMembers.isEmpty()) return

        for (ktEntry in ktSuperTypeList.entries) {
            if (ktEntry is KtDelegatedSuperTypeEntry) {
                generateDelegatedImplementationMembers(irClass, ktEntry, delegatedMembers)
            }
        }
    }

    private fun generateDelegatedImplementationMembers(
        irClass: IrClass,
        ktEntry: KtDelegatedSuperTypeEntry,
        delegatedMembers: List<CallableMemberDescriptor>
    ) {
        val ktDelegateExpression = ktEntry.delegateExpression!!
        val delegateType = getInferredTypeWithImplicitCastsOrFail(ktDelegateExpression)
        val superType = getOrFail(BindingContext.TYPE, ktEntry.typeReference!!)
        val superTypeConstructorDescriptor = superType.constructor.declarationDescriptor
        val superClass = superTypeConstructorDescriptor as? ClassDescriptor
                ?: throw AssertionError("Unexpected supertype constructor for delegation: $superTypeConstructorDescriptor")
        val delegateDescriptor = IrImplementingDelegateDescriptorImpl(irClass.descriptor, delegateType, superType)
        val irDelegateField = context.symbolTable.declareField(
            ktDelegateExpression.startOffset, ktDelegateExpression.endOffset,
            IrDeclarationOrigin.DELEGATE,
            delegateDescriptor, delegateDescriptor.type.toIrType(),
            createBodyGenerator(irClass.symbol).generateExpressionBody(ktDelegateExpression)
        )
        irClass.addMember(irDelegateField)

        for (delegatedMember in delegatedMembers) {
            val overriddenMember = delegatedMember.overriddenDescriptors.find { it.containingDeclaration.original == superClass.original }
            if (overriddenMember != null) {
                generateDelegatedMember(irClass, irDelegateField, delegatedMember, overriddenMember)
            }
        }
    }

    private fun generateDelegatedMember(
        irClass: IrClass,
        irDelegate: IrField,
        delegatedMember: CallableMemberDescriptor,
        overriddenMember: CallableMemberDescriptor
    ) {
        when (delegatedMember) {
            is FunctionDescriptor ->
                generateDelegatedFunction(irClass, irDelegate, delegatedMember, overriddenMember as FunctionDescriptor)
            is PropertyDescriptor ->
                generateDelegatedProperty(irClass, irDelegate, delegatedMember, overriddenMember as PropertyDescriptor)
        }

    }

    private fun generateDelegatedProperty(
        irClass: IrClass,
        irDelegate: IrField,
        delegated: PropertyDescriptor,
        overridden: PropertyDescriptor
    ) {
        irClass.addMember(generateDelegatedProperty(irDelegate, delegated, overridden))
    }

    private fun generateDelegatedProperty(
        irDelegate: IrField,
        delegatedDescriptor: PropertyDescriptor,
        overriddenDescriptor: PropertyDescriptor
    ): IrPropertyImpl {
        val startOffset = irDelegate.startOffset
        val endOffset = irDelegate.endOffset

        val irProperty = IrPropertyImpl(
            startOffset, endOffset, IrDeclarationOrigin.DELEGATED_MEMBER,
            false, delegatedDescriptor
        )

        irProperty.getter = generateDelegatedFunction(irDelegate, delegatedDescriptor.getter!!, overriddenDescriptor.getter!!)

        if (delegatedDescriptor.isVar) {
            irProperty.setter = generateDelegatedFunction(irDelegate, delegatedDescriptor.setter!!, overriddenDescriptor.setter!!)
        }
        return irProperty
    }

    private fun generateDelegatedFunction(
        irClass: IrClass,
        irDelegate: IrField,
        delegated: FunctionDescriptor,
        overridden: FunctionDescriptor
    ) {
        irClass.addMember(generateDelegatedFunction(irDelegate, delegated, overridden))
    }

    private fun generateDelegatedFunction(
        irDelegate: IrField,
        delegated: FunctionDescriptor,
        overridden: FunctionDescriptor
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            irDelegate.startOffset, irDelegate.endOffset,
            IrDeclarationOrigin.DELEGATED_MEMBER,
            delegated
        ).buildWithScope { irFunction ->
            FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)

            // TODO could possibly refer to scoped type parameters for property accessors
            irFunction.returnType = delegated.returnType!!.toIrType()

            irFunction.body = generateDelegateFunctionBody(irDelegate, delegated, overridden, irFunction)
        }

    private fun generateDelegateFunctionBody(
        irDelegate: IrField,
        delegated: FunctionDescriptor,
        overridden: FunctionDescriptor,
        irDelegatedFunction: IrSimpleFunction
    ): IrBlockBodyImpl {
        val startOffset = irDelegate.startOffset
        val endOffset = irDelegate.endOffset
        val irBlockBody = IrBlockBodyImpl(startOffset, endOffset)
        val substitutedOverridden = substituteOverriddenDescriptorForDelegate(delegated, overridden)
        val returnType = substitutedOverridden.returnType!!
        val irReturnType = returnType.toIrType()
        val irCall = IrCallImpl(
            startOffset, endOffset, irReturnType,
            context.symbolTable.referenceFunction(overridden.original),
            substitutedOverridden,
            substitutedOverridden.typeParametersCount
        ).apply {
            val typeArguments = getTypeArgumentsForOverriddenDescriptorDelegatingCall(delegated, overridden)
            putTypeArguments(typeArguments) { it.toIrType() }
        }
        val dispatchReceiverParameter = irDelegatedFunction.dispatchReceiverParameter!!
        val dispatchReceiverType = dispatchReceiverParameter.type
        irCall.dispatchReceiver =
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irDelegate.symbol,
                    irDelegate.type,
                    IrGetValueImpl(
                        startOffset, endOffset,
                        dispatchReceiverType,
                        dispatchReceiverParameter.symbol
                    )
                )
        irCall.extensionReceiver =
                irDelegatedFunction.extensionReceiverParameter?.let { extensionReceiver ->
                    IrGetValueImpl(startOffset, endOffset, extensionReceiver.type, extensionReceiver.symbol)
                }
        irCall.mapValueParameters { overriddenValueParameter ->
            val delegatedValueParameter = delegated.valueParameters[overriddenValueParameter.index]
            val irDelegatedValueParameter = irDelegatedFunction.getIrValueParameter(delegatedValueParameter)
            IrGetValueImpl(startOffset, endOffset, irDelegatedValueParameter.type, irDelegatedValueParameter.symbol)
        }
        if (KotlinBuiltIns.isUnit(returnType) || KotlinBuiltIns.isNothing(returnType)) {
            irBlockBody.statements.add(irCall)
        } else {
            val irReturn = IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, irDelegatedFunction.symbol, irCall)
            irBlockBody.statements.add(irReturn)
        }
        return irBlockBody
    }

    private fun substituteOverriddenDescriptorForDelegate(
        delegated: FunctionDescriptor,
        overridden: FunctionDescriptor
    ): FunctionDescriptor {
        // TODO PropertyAccessorDescriptor doesn't support 'substitute' right now :(
        return if (overridden is PropertyAccessorDescriptor)
            overridden
        else {
            val substitutor =
                TypeSubstitutor.create(
                    overridden.typeParameters.associate {
                        val delegatedDefaultType = delegated.typeParameters[it.index].defaultType
                        it.typeConstructor to TypeProjectionImpl(delegatedDefaultType)
                    }
                )
            overridden.substitute(substitutor)!!
        }
    }

    private fun getTypeArgumentsForOverriddenDescriptorDelegatingCall(
        delegated: FunctionDescriptor,
        overridden: FunctionDescriptor
    ): Map<TypeParameterDescriptor, KotlinType>? =
        if (overridden.original.typeParameters.isEmpty())
            null
        else
            zipTypeParametersToDefaultTypes(overridden.original, delegated)

    private fun zipTypeParametersToDefaultTypes(
        keys: FunctionDescriptor,
        values: FunctionDescriptor
    ): Map<TypeParameterDescriptor, KotlinType> {
        val typeArguments = newHashMapWithExpectedSize<TypeParameterDescriptor, KotlinType>(keys.typeParameters.size)
        for ((i, overriddenTypeParameter) in keys.typeParameters.withIndex()) {
            typeArguments[overriddenTypeParameter] = values.typeParameters[i].defaultType
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
        ktClassOrObject.getBody()?.let { ktClassBody ->
            ktClassBody.declarations.mapTo(irClass.declarations) { ktDeclaration ->
                declarationGenerator.generateClassMemberDeclaration(ktDeclaration, irClass.descriptor)
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
        return context.symbolTable.declareEnumEntry(
            ktEnumEntry.startOffset,
            ktEnumEntry.endOffset,
            IrDeclarationOrigin.DEFINED,
            enumEntryDescriptor
        ).buildWithScope { irEnumEntry ->
            if (!enumEntryDescriptor.isExpect) {
                irEnumEntry.initializerExpression =
                        createBodyGenerator(irEnumEntry.symbol)
                            .generateEnumEntryInitializer(ktEnumEntry, enumEntryDescriptor)
            }

            if (ktEnumEntry.declarations.isNotEmpty()) {
                irEnumEntry.correspondingClass = generateClass(ktEnumEntry)
            }
        }

    }
}
