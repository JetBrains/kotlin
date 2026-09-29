/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.builder

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

// These mirror `org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder.updateFrom` exactly, so code migrating
// off that builder keeps its behaviour. They are hand-written rather than generated because they are not meant to
// outlive that migration. Like the original, they copy neither `name` nor `returnType`.
//
// A member always wins over an extension: with an argument statically typed as `IrSimpleFunction` or `IrConstructor`,
// the generated member `updateFrom` is called instead, and that one also copies `name`.

@DeprecatedCompilerApi(
    deprecatedSince = CompilerVersionOfApiDeprecation._2_5_20,
    message = "Only kept for code migrating off IrFunctionBuilder. Use updateFrom(IrSimpleFunction) or set the properties explicitly.",
)
fun IrSimpleFunctionBuilder.updateFrom(from: IrFunction) {
    startOffset = from.startOffset
    endOffset = from.endOffset
    origin = from.origin
    visibility = from.visibility

    containerSource = from.containerSource

    isInline = from.isInline
    isExternal = from.isExternal
    isExpect = from.isExpect

    if (from is IrSimpleFunction) {
        modality = from.modality
        isTailrec = from.isTailrec
        isSuspend = from.isSuspend
        isOperator = from.isOperator
        isInfix = from.isInfix
        isFakeOverride = from.isFakeOverride
    } else {
        modality = Modality.FINAL
        isTailrec = false
        isSuspend = false
        isOperator = false
        isInfix = false
    }
}

@DeprecatedCompilerApi(
    deprecatedSince = CompilerVersionOfApiDeprecation._2_5_20,
    message = "Only kept for code migrating off IrFunctionBuilder. Use updateFrom(IrConstructor) or set the properties explicitly.",
)
fun IrConstructorBuilder.updateFrom(from: IrFunction) {
    startOffset = from.startOffset
    endOffset = from.endOffset
    origin = from.origin
    visibility = from.visibility

    containerSource = from.containerSource

    isInline = from.isInline
    isExternal = from.isExternal
    isExpect = from.isExpect

    if (from is IrConstructor) {
        isPrimary = from.isPrimary
    }
}
