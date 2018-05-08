/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name

object InlineClassDescriptorResolver {
    @JvmField
    val BOX_METHOD_NAME = Name.identifier("box")

    @JvmField
    val UNBOX_METHOD_NAME = Name.identifier("unbox")

    private val BOXING_VALUE_PARAMETER_NAME = Name.identifier("v")

    fun createBoxFunctionDescriptor(owner: ClassDescriptor): SimpleFunctionDescriptor? {
        return createConversionFunctionDescriptor(true, owner)
    }

    fun createUnboxFunctionDescriptor(owner: ClassDescriptor): SimpleFunctionDescriptor? =
        createConversionFunctionDescriptor(false, owner)

    private fun createConversionFunctionDescriptor(
        isBoxMethod: Boolean,
        owner: ClassDescriptor
    ): SimpleFunctionDescriptor? {
        val inlinedValue = owner.underlyingRepresentation() ?: return null

        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            if (isBoxMethod) BOX_METHOD_NAME else UNBOX_METHOD_NAME,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )

        functionDescriptor.initialize(
            null,
            if (isBoxMethod) null else owner.thisAsReceiverParameter,
            emptyList<TypeParameterDescriptor>(),
            if (isBoxMethod) listOf(createValueParameterForBoxing(functionDescriptor, inlinedValue)) else emptyList(),
            if (isBoxMethod) owner.defaultType else inlinedValue.returnType,
            Modality.FINAL,
            Visibilities.PUBLIC
        )

        return functionDescriptor
    }

    private fun createValueParameterForBoxing(
        functionDescriptor: FunctionDescriptor,
        inlinedValue: ValueParameterDescriptor
    ): ValueParameterDescriptorImpl {
        return ValueParameterDescriptorImpl(
            functionDescriptor,
            null,
            0,
            Annotations.EMPTY,
            BOXING_VALUE_PARAMETER_NAME,
            inlinedValue.type,
            false, false, false, null, SourceElement.NO_SOURCE
        )
    }
}