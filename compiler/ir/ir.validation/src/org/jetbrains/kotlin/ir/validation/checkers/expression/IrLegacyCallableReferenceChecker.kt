/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

/**
 * Makes sure that no legacy callable reference nodes are present in the IR on the first stage of KLIB-based compilation.
 */
object IrLegacyCallableReferenceChecker : IrElementChecker<IrCallableReference<*>>(IrCallableReference::class) {
    override fun check(element: IrCallableReference<*>, context: CheckerContext) {
        val [legacyNode, richNode] = when (element) {
            is IrFunctionReference -> "IrFunctionReference" to "IrRichFunctionReference"
            is IrPropertyReference -> "IrPropertyReference" to "IrRichPropertyReference"
            is IrLocalDelegatedPropertyReference -> "IrLocalDelegatedPropertyReference" to "IrRichPropertyReference"
            else -> return
        }
        context.error(
            element,
            "Legacy callable reference node '$legacyNode' cannot be used on the first stage of KLIB-based compilation. " +
                    "Generate '$richNode' instead. " +
                    "To temporarily suppress this check, pass '-Xdisable-ir-checkers=${this::class.simpleName}'.",
        )
    }
}
