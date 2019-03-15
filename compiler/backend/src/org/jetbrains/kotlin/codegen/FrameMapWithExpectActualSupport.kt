/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver

/**
 * This [FrameMap] subclass substitutes values declared in the expected declaration with the corresponding value in the actual declaration,
 * which is needed for the case when expected function declares parameters with default values, which refer to other parameters.
 */
class FrameMapWithExpectActualSupport(private val module: ModuleDescriptor) : FrameMap() {
    override fun getIndex(descriptor: DeclarationDescriptor): Int {
        val tmp = if (descriptor is ParameterDescriptor) findActualParameter(descriptor) ?: descriptor else descriptor
        return super.getIndex(tmp)
    }

    private fun findActualParameter(parameter: ParameterDescriptor): ParameterDescriptor? {
        val container = parameter.containingDeclaration
        if (container !is CallableMemberDescriptor || !container.isExpect) return null

        // Generation of value parameters is supported by the fact that FunctionCodegen.generateDefaultImplBody substitutes value parameters
        // of the generated actual function with the parameters of the expected declaration in the first place.
        // Generation of dispatch receiver parameters (this and outer receiver values) is supported
        // in ExpressionCodegen.generateThisOrOuterFromContext by comparing classes by type constructor equality.
        if (parameter !is ReceiverParameterDescriptor || parameter.value !is ExtensionReceiver) return null

        val actual = with(ExpectedActualResolver) {
            container.findCompatibleActualForExpected(module).firstOrNull()
        }

        return (actual as? CallableDescriptor)?.extensionReceiverParameter
    }
}
