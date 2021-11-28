/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.loadCompiledInlineFunction
import org.jetbrains.kotlin.codegen.optimization.nullCheck.usesLocalExceptParameterNullCheck
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource

class DelegatedPropertiesCodegenHelper(private val state: GenerationState) {

    private val bindingContext = state.bindingContext
    private val bindingTrace = state.bindingTrace
    private val typeMapper = state.typeMapper

    fun isDelegatedPropertyMetadataRequired(descriptor: VariableDescriptorWithAccessors): Boolean {
        val provideDelegateResolvedCall = bindingContext[BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL, descriptor]
        val getValueResolvedCall = descriptor.getter?.let { bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, it] }
        val setValueResolvedCall = descriptor.setter?.let { bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, it] }

        return provideDelegateResolvedCall != null && isDelegatedPropertyMetadataRequired(provideDelegateResolvedCall) ||
                getValueResolvedCall != null && isDelegatedPropertyMetadataRequired(getValueResolvedCall) ||
                setValueResolvedCall != null && isDelegatedPropertyMetadataRequired(setValueResolvedCall)
    }

    private fun isDelegatedPropertyMetadataRequired(operatorCall: ResolvedCall<FunctionDescriptor>): Boolean {
        val calleeDescriptor = operatorCall.resultingDescriptor.getActualCallee().original

        if (!calleeDescriptor.isInline) return true

        val metadataParameter = calleeDescriptor.valueParameters[1]
        if (true == bindingContext[BindingContext.UNUSED_DELEGATED_PROPERTY_OPERATOR_PARAMETER, metadataParameter]) {
            return false
        }

        if (calleeDescriptor !is DescriptorWithContainerSource) return true

        val cachedResult = bindingTrace[CodegenBinding.PROPERTY_METADATA_REQUIRED_FOR_OPERATOR_CALL, calleeDescriptor]
        if (cachedResult != null) {
            return cachedResult
        }

        return isDelegatedPropertyMetadataRequiredForFunctionFromBinaries(calleeDescriptor).also {
            bindingTrace.record(CodegenBinding.PROPERTY_METADATA_REQUIRED_FOR_OPERATOR_CALL, calleeDescriptor, it)
        }
    }

    private fun FunctionDescriptor.getActualCallee(): FunctionDescriptor =
        if (this is FunctionImportedFromObject)
            callableFromObject
        else
            this

    private fun isDelegatedPropertyMetadataRequiredForFunctionFromBinaries(calleeDescriptor: FunctionDescriptor): Boolean {
        require(calleeDescriptor is DescriptorWithContainerSource) {
            "Function descriptor from binaries expected: $calleeDescriptor"
        }
        val metadataParameterIndex = getMetadataParameterIndex(calleeDescriptor)
        val containerId = KotlinTypeMapper.getContainingClassesForDeserializedCallable(calleeDescriptor).implClassId
        val asmMethod = state.typeMapper.mapAsmMethod(calleeDescriptor)
        val isMangled = requiresFunctionNameManglingForReturnType(calleeDescriptor)
        val methodNode = loadCompiledInlineFunction(containerId, asmMethod, calleeDescriptor.isSuspend, isMangled, state).node
        return methodNode.usesLocalExceptParameterNullCheck(metadataParameterIndex)
    }

    private fun getMetadataParameterIndex(calleeDescriptor: FunctionDescriptor): Int {
        assert(calleeDescriptor.valueParameters.size >= 2) {
            "Unexpected delegated property operator (should have at least 2 value parameters): $calleeDescriptor"
        }

        var index = 0

        calleeDescriptor.dispatchReceiverParameter?.let {
            index += typeMapper.mapType(it.type).size
        }

        calleeDescriptor.extensionReceiverParameter?.let {
            index += typeMapper.mapType(it.type).size
        }

        index += typeMapper.mapType(calleeDescriptor.valueParameters[0].type).size

        return index
    }
}
