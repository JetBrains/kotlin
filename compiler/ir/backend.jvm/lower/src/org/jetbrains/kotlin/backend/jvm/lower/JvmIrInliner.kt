/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver

@PhaseDescription(
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = [JvmExpectDeclarationRemover::class, JvmInlineCallableReferenceToLambdaWithDefaultsPhase::class]
)
class JvmIrInliner(context: JvmBackendContext) : FunctionInlining(
    context,
    inlineFunctionResolver = JvmInlineFunctionResolver(context),
    regenerateInlinedAnonymousObjects = true,
) {
    private val enabled = context.config.enableIrInliner

    override fun lower(irFile: IrFile) {
        if (enabled) {
            super.lower(irFile)
        }
    }
}

class JvmInlineFunctionResolver(private val context: JvmBackendContext) : InlineFunctionResolver() {
    override fun needsInlining(function: IrFunction): Boolean = function.isInlineFunctionCall(context)
}
