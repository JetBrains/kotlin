/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.codegen.inline.parameterOffsets
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature

fun extractDefaultLambdaOffsetAndDescriptor(
    jvmSignature: JvmMethodSignature,
    irFunction: IrFunction
): Map<Int, IrValueParameter> {
    val valueParameters = jvmSignature.valueParameters
    val parameterOffsets = parameterOffsets(irFunction.isStatic, valueParameters)

    val valueParameterOffset = if (irFunction.extensionReceiverParameter != null) 1 else 0

    return irFunction.valueParameters.filter {
        it.defaultValue != null && it.isInlineParameter(it.defaultValue!!.expression.type)
    }.associateBy {
        parameterOffsets[valueParameterOffset + it.index]
    }
}
