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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegationResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils.createErrorType
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

@ObsoleteDescriptorBasedAPI
internal class ClassGenerator(
    declarationGenerator: DeclarationGenerator
) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generateClass(ktClassOrObject: KtPureClassOrObject, visibility_: DescriptorVisibility? = null): IrClass {
        val classDescriptor = ktClassOrObject.findClassDescriptor(this.context.bindingContext)
        val startOffset = ktClassOrObject.getStartOffsetOfClassDeclarationOrNull() ?: ktClassOrObject.pureStartOffset
        val endOffset = ktClassOrObject.pureEndOffset
        val visibility = visibility_ ?: classDescriptor.visibility
        val modality = getEffectiveModality(ktClassOrObject, classDescriptor)

        return context.symbolTable.declareClass(classDescriptor) {
            context.irFactory.createIrClassFromDescriptor(
                startOffset, endOffset, IrDeclarationOrigin.DEFINED, it, classDescriptor,
                context.symbolTable.nameProvider.nameForDeclaration(classDescriptor), visibility, modality
            ).apply {
                metadata = DescriptorMetadataSource.Class(it.descriptor)
            }
        }.buildWithScope { irClass ->
            declarationGenerator.generateGlobalTypeParametersDeclarations(irClass, classDescriptor.declaredTypeParameters)

            irClass.superTypes = classDescriptor.typeConstructor.supertypes.map {
                it.toIrType()
            }

            irClass.thisReceiver = context.symbolTable.declareValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                classDescriptor.thisAsReceiverParameter,
                classDescriptor.thisAsReceiverParameter.type.toIrType()
            )

            generateFieldsForContextReceivers(irClass, classDescriptor)

            val irPrimaryConstructor = generatePrimaryConstructor(irClass, ktClassOrObject)
            if (irPrimaryConstructor != null) {
                generateDeclarationsForPrimaryConstructorParameters(irClass, irPrimaryConstructor, ktClassOrObject)
            }

            if (ktClassOrObject is KtClassOrObject) //todo: supertype list for synthetic declarations
                generateMembersDeclaredInSupertypeList(irClass, ktClassOrObject)

            generateMembersDeclaredInClassBody(irClass, ktClassOrObject)

            generateFakeOverrideMemberDeclarations(irClass, ktClassOrObject)

            irClass.valueClassRepresentation = classDescriptor.valueClassRepresentation?.mapUnderlyingType { type ->
                type.toIrType() as? IrSimpleType ?: error("Value class underlying type is not a simple type: $classDescriptor")
            }

            if (irClass.isSingleFieldValueClass && ktClassOrObject is KtClassOrObject) {
                generateAdditionalMembersForSingleFieldValueClasses(irClass, ktClassOrObject)
            }

            if (irClass.isData && ktClassOrObject is KtClassOrObject) {
                generateAdditionalMembersForDataClass(irClass, ktClassOrObject)
            }

            if (irClass.isMultiFieldValueClass && ktClassOrObject is KtClassOrObject) {
                generateAdditionalMembersForMultiFieldValueClasses(irClass, ktClassOrObject)
            }

            if (DescriptorUtils.isEnumClass(classDescriptor)) {
                generateAdditionalMembersForEnumClass(irClass)
            }

            irClass.sealedSubclasses = classDescriptor.sealedSubclasses.map { context.symbolTable.referenceClass(it) }
        }
    }

    private fun getEffectiveModality(ktClassOrObject: KtPureClassOrObject, classDescriptor: ClassDescriptor): Modality =
        when {
            DescriptorUtils.isAnnotationClass(classDescriptor) ->
                Modality.OPEN
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
        val classDescriptor = irClass.descriptor

        for (descriptor in classDescriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
            if (descriptor is CallableMemberDescriptor && descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                declarationGenerator.generateFakeOverrideDeclaration(descriptor, ktClassOrObject)?.let { irClass.declarations.add(it) }
            }
        }

        context.extensions.getParentClassStaticScope(classDescriptor)?.run {
            for (parentStaticMember in getContributedDescriptors()) {
                if (parentStaticMember is FunctionDescriptor &&
                    DescriptorVisibilityUtils.isVisibleIgnoringReceiver(
                        parentStaticMember,
                        classDescriptor,
                        context.languageVersionSettings
                    )
                ) {
                    val fakeOverride = createFakeOverrideDescriptorForParentStaticMember(classDescriptor, parentStaticMember)
                    declarationGenerator.generateFakeOverrideDeclaration(fakeOverride, ktClassOrObject)?.let {
                        irClass.declarations.add(it)
                    }
                }
            }
        }
    }

    private fun createFakeOverrideDescriptorForParentStaticMember(
        classDescriptor: ClassDescriptor,
        parentStaticMember: FunctionDescriptor
    ): FunctionDescriptor =
        parentStaticMember.copy(
            classDescriptor, parentStaticMember.modality, parentStaticMember.visibility, CallableMemberDescriptor.Kind.FAKE_OVERRIDE, false
        ).apply {
            setSingleOverridden(parentStaticMember)
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
        val delegateType = if (context.configuration.generateBodies) {
            getTypeInferredByFrontendOrFail(ktDelegateExpression)
        } else {
            getTypeInferredByFrontend(ktDelegateExpression) ?: createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, ktDelegateExpression.text)
        }
        val superType = getOrFail(BindingContext.TYPE, ktEntry.typeReference!!)

        val superTypeConstructorDescriptor = superType.constructor.declarationDescriptor
        val superClass = superTypeConstructorDescriptor as? ClassDescriptor
            ?: throw AssertionError("Unexpected supertype constructor for delegation: $superTypeConstructorDescriptor")

        val propertyDescriptor = CodegenUtil.getDelegatePropertyIfAny(ktDelegateExpression, irClass.descriptor, context.bindingContext)

        val irDelegateField: IrField = if (CodegenUtil.isFinalPropertyWithBackingField(propertyDescriptor, context.bindingContext)) {
            irClass.properties.first { it.descriptor == propertyDescriptor }.backingField!!
        } else {
            val delegateDescriptor = IrImplementingDelegateDescriptorImpl(irClass.descriptor, delegateType, superType, delegateNumber)
            val initializer = if (context.configuration.generateBodies) {
                createBodyGenerator(irClass.symbol).generateExpressionBody(ktDelegateExpression)
            } else {
                null
            }
            context.symbolTable.declareField(
                ktDelegateExpression.startOffsetSkippingComments, ktDelegateExpression.endOffset,
                IrDeclarationOrigin.DELEGATE,
                delegateDescriptor, delegateDescriptor.type.toIrType(),
                initializer
            ).apply {
                irClass.addMember(this)
            }
        }

        val delegatesMap = DelegationResolver.getDelegates(irClass.descriptor, superClass, delegateType)
        for (delegatedMember in delegatesMap.keys) {
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

        irProperty.generateOverrides(delegatedDescriptor)

        irProperty.linkCorrespondingPropertySymbol()

        return irProperty
    }

    private fun IrProperty.generateOverrides(propertyDescriptor: PropertyDescriptor) {
        overriddenSymbols =
            propertyDescriptor.overriddenDescriptors.map { overriddenPropertyDescriptor ->
                context.symbolTable.referenceProperty(overriddenPropertyDescriptor.original)
            }
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
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            IrDeclarationOrigin.DELEGATED_MEMBER,
            delegatedDescriptor
        ).buildWithScope { irFunction ->
            FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)

            // TODO could possibly refer to scoped type parameters for property accessors
            irFunction.returnType = delegatedDescriptor.returnType!!.toIrType()

            if (context.configuration.generateBodies) {
                irFunction.body = generateDelegateFunctionBody(irDelegate, delegatedDescriptor, delegateToDescriptor, irFunction)
            }
        }

    private fun generateDelegateFunctionBody(
        irDelegate: IrField,
        delegatedDescriptor: FunctionDescriptor,
        delegateToDescriptor: FunctionDescriptor,
        irDelegatedFunction: IrSimpleFunction
    ): IrBlockBody {
        val startOffset = SYNTHETIC_OFFSET
        val endOffset = SYNTHETIC_OFFSET

        val irBlockBody = context.irFactory.createBlockBody(startOffset, endOffset)

        val substitutedDelegateTo = substituteDelegateToDescriptor(delegatedDescriptor, delegateToDescriptor)
        val returnType = substitutedDelegateTo.returnType!!

        val delegateToSymbol = context.symbolTable.referenceSimpleFunction(delegateToDescriptor.original)

        val irCall = IrCallImpl.fromSymbolDescriptor(
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

            (delegatedDescriptor.contextReceiverParameters + delegatedDescriptor.valueParameters).forEachIndexed { index, param ->
                val delegatedParameter = irDelegatedFunction.getIrValueParameter(param, index)
                putValueArgument(index, IrGetValueImpl(startOffset, endOffset, delegatedParameter.type, delegatedParameter.symbol))
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

    private fun generateAdditionalMembersForSingleFieldValueClasses(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(declarationGenerator, context.configuration.generateBodies).generateSingleFieldValueClassMembers(ktClassOrObject, irClass)
    }

    private fun generateAdditionalMembersForMultiFieldValueClasses(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(declarationGenerator, context.configuration.generateBodies).generateMultiFieldValueClassMembers(ktClassOrObject, irClass)
    }

    private fun generateAdditionalMembersForDataClass(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(declarationGenerator, context.configuration.generateBodies).generateDataClassMembers(ktClassOrObject, irClass)
    }

    private fun generateAdditionalMembersForEnumClass(irClass: IrClass) {
        EnumClassMembersGenerator(declarationGenerator).generateSpecialMembers(irClass)
    }

    private fun generateFieldsForContextReceivers(irClass: IrClass, classDescriptor: ClassDescriptor) {
        for ((fieldIndex, receiverDescriptor) in classDescriptor.contextReceivers.withIndex()) {
            val irField = context.irFactory.createField(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.FIELD_FOR_CLASS_CONTEXT_RECEIVER,
                IrFieldSymbolImpl(),
                Name.identifier("contextReceiverField$fieldIndex"),
                receiverDescriptor.type.toIrType(),
                DescriptorVisibilities.PRIVATE,
                isFinal = true,
                isExternal = false,
                isStatic = false
            )
            context.additionalDescriptorStorage.put(receiverDescriptor.value, irField)
            irClass.addMember(irField)
        }
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
                val irValueParameter = irPrimaryConstructor.valueParameters[i + ktClassOrObject.contextReceivers.size]
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
                declarationGenerator.generateClassMemberDeclaration(ktDeclaration, irClass, ktClassOrObject)
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
                    context.irFactory.createExpressionBody(
                        createBodyGenerator(irEnumEntry.symbol)
                            .generateEnumEntryInitializer(ktEnumEntry, enumEntryDescriptor)
                    )
            }

            if (ktEnumEntry.hasMemberDeclarations()) {
                irEnumEntry.correspondingClass = generateClass(ktEnumEntry, DescriptorVisibilities.PRIVATE)
            }
        }

    }
}
