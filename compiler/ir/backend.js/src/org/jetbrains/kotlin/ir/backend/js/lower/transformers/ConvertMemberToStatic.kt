/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.transformers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.copyValueParametersToStatic
import org.jetbrains.kotlin.ir.util.createStaticFunctionWithReceivers
import org.jetbrains.kotlin.ir.util.isOriginallyLocal
import org.jetbrains.kotlin.ir.util.parentAsClass

context(context: CommonBackendContext)
fun transformMemberToStaticFunction(function: IrSimpleFunction, origin: IrDeclarationOrigin = function.origin): IrSimpleFunction {
    val parentClass = function.parentAsClass
    val newVisibility = if (function.isOriginallyLocal) DescriptorVisibilities.LOCAL else function.visibility
    val typeParametersFromContext = extractTypeParameters(function.parentAsClass)
    return context.irFactory.createStaticFunctionWithReceivers(
        parentClass,
        function.name,
        function,
        origin = origin,
        visibility = newVisibility,
        typeParametersFromContext = typeParametersFromContext,
        remapMultiFieldValueClassStructure = context::remapMultiFieldValueClassStructure
    ).also {
        // TODO: move it inside createStaticFunctionWithReceivers
        it.parameters = emptyList()
        it.copyValueParametersToStatic(
            function,
            origin,
            customTypeParameterMapping = (typeParametersFromContext + function.typeParameters)
                .zip(it.typeParameters).toMap()
        )
        it.body = function.moveBodyTo(it)
    }
}