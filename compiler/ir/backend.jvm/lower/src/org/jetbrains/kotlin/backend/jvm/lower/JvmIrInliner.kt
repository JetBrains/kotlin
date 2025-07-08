/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.InlineMode
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmInlineFunctionResolver
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.util.JvmIrInlineExperimental

@PhaseDescription(
    name = "FunctionInliningPhase",
    prerequisite = [JvmExpectDeclarationRemover::class, JvmInlineCallableReferenceToLambdaWithDefaultsPhase::class]
)
class JvmIrInliner(context: JvmBackendContext) : FileLoweringPass {
    @OptIn(JvmIrInlineExperimental::class)
    val inliner = FunctionInlining(
        context,
        inlineMode = InlineMode.ALL_INLINE_FUNCTIONS,
        inlineFunctionResolver = JvmInlineFunctionResolver(context),
        regenerateInlinedAnonymousObjects = true,
        insertAdditionalImplicitCasts = false,
        produceOuterThisFields = true
    )
    private val enabled = context.config.enableIrInliner

    override fun lower(irFile: IrFile) {
        if (enabled) {
            inliner.lower(irFile)
        }
    }
}
