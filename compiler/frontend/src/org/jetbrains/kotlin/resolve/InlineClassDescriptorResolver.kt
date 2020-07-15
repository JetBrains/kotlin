/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType

object InlineClassDescriptorResolver {
    @JvmField
    val BOX_METHOD_NAME = Name.identifier("box")

    @JvmField
    val UNBOX_METHOD_NAME = Name.identifier("unbox")

    @JvmField
    val SPECIALIZED_EQUALS_NAME = Name.identifier("equals-impl0")

    val BOXING_VALUE_PARAMETER_NAME = Name.identifier("v")

    val SPECIALIZED_EQUALS_FIRST_PARAMETER_NAME = Name.identifier("p1")
    val SPECIALIZED_EQUALS_SECOND_PARAMETER_NAME = Name.identifier("p2")

    @JvmStatic
    fun createBoxFunctionDescriptor(owner: ClassDescriptor): SimpleFunctionDescriptor =
        createConversionFunctionDescriptor(true, owner)

    @JvmStatic
    fun createUnboxFunctionDescriptor(owner: ClassDescriptor): SimpleFunctionDescriptor =
        createConversionFunctionDescriptor(false, owner)

    @JvmStatic
    fun isSynthesizedBoxMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMemberWithName(descriptor, BOX_METHOD_NAME)

    @JvmStatic
    fun isSynthesizedUnboxMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMemberWithName(descriptor, UNBOX_METHOD_NAME)

    @JvmStatic
    fun isSynthesizedBoxOrUnboxMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMember(descriptor) && (descriptor.name == BOX_METHOD_NAME || descriptor.name == UNBOX_METHOD_NAME)

    @JvmStatic
    fun isSpecializedEqualsMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMemberWithName(descriptor, SPECIALIZED_EQUALS_NAME)

    private fun isSynthesizedInlineClassMemberWithName(descriptor: CallableMemberDescriptor, name: Name) =
        isSynthesizedInlineClassMember(descriptor) && descriptor.name == name

    private fun isSynthesizedInlineClassMember(descriptor: CallableMemberDescriptor) =
        descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED && descriptor.containingDeclaration.isInlineClass()

    fun createSpecializedEqualsDescriptor(owner: ClassDescriptor): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            SPECIALIZED_EQUALS_NAME,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )

        functionDescriptor.initialize(
            null,
            null,
            emptyList<ReceiverParameterDescriptor>(),
            emptyList<TypeParameterDescriptor>(),
            createValueParametersForSpecializedEquals(functionDescriptor, owner.inlineClassRepresentation!!.underlyingType),
            owner.builtIns.booleanType,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )

        return functionDescriptor
    }

    private fun createConversionFunctionDescriptor(isBoxMethod: Boolean, owner: ClassDescriptor): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            if (isBoxMethod) BOX_METHOD_NAME else UNBOX_METHOD_NAME,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )

        val underlyingType = owner.inlineClassRepresentation!!.underlyingType
        functionDescriptor.initialize(
            null,
            if (isBoxMethod) null else owner.thisAsReceiverParameter,
            emptyList<ReceiverParameterDescriptor>(),
            emptyList<TypeParameterDescriptor>(),
            if (isBoxMethod) listOf(createValueParameterForBoxing(functionDescriptor, underlyingType)) else emptyList(),
            if (isBoxMethod) owner.defaultType else underlyingType,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )

        return functionDescriptor
    }

    private fun createValueParameterForBoxing(
        functionDescriptor: FunctionDescriptor, underlyingType: KotlinType
    ): ValueParameterDescriptorImpl =
        createValueParameter(functionDescriptor, underlyingType, BOXING_VALUE_PARAMETER_NAME, 0)

    private fun createValueParametersForSpecializedEquals(
        functionDescriptor: FunctionDescriptor, underlyingType: KotlinType
    ): List<ValueParameterDescriptor> =
        listOf(
            createValueParameter(functionDescriptor, underlyingType, SPECIALIZED_EQUALS_FIRST_PARAMETER_NAME, 0),
            createValueParameter(functionDescriptor, underlyingType, SPECIALIZED_EQUALS_SECOND_PARAMETER_NAME, 1)
        )

    private fun createValueParameter(
        functionDescriptor: FunctionDescriptor, type: KotlinType, name: Name, index: Int
    ): ValueParameterDescriptorImpl =
        ValueParameterDescriptorImpl(
            functionDescriptor, null, index, Annotations.EMPTY, name, type, false, false, false, null, SourceElement.NO_SOURCE
        )
}
