/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.inline.CommonInlineCallableReferenceToLambdaPhase
import org.jetbrains.kotlin.ir.inline.InlineCallableReferenceToLambdaPhase

@PhaseDescription(name = "JvmInlineCallableReferenceToLambdaWithDefaultsPhase")
internal class JvmInlineCallableReferenceToLambdaWithDefaultsPhase(
    context: JvmBackendContext,
) : CommonInlineCallableReferenceToLambdaPhase(
    context, JvmInlineFunctionResolver(context)
) {
    private val enabled = context.config.enableIrInliner

    override fun lower(irFile: IrFile) {
        if (enabled) {
            super.lower(irFile)
        }
    }

    // Don't transform a function reference if it is not an argument for an inline function
    override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent?): IrElement {
        expression.transformChildren(this, data)
        return expression
    }
}

/**
 * Transforms callable references to inline lambdas, marks inline lambdas for later passes.
 */
@PhaseDescription(name = "JvmInlineCallableReferenceToLambdaPhase")
internal class JvmInlineCallableReferenceToLambdaPhase(
    context: JvmBackendContext,
) : InlineCallableReferenceToLambdaPhase(
    context, JvmInlineFunctionResolver(context)
)
