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
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.AbstractInlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

@PhaseDescription(
    name = "FunctionInliningPhase",
    prerequisite = [JvmExpectDeclarationRemover::class, JvmInlineCallableReferenceToLambdaWithDefaultsPhase::class]
)
class JvmIrInliner(context: JvmBackendContext) : FunctionInlining(
    context,
    inlineFunctionResolver = JvmInlineFunctionResolver(context),
    regenerateInlinedAnonymousObjects = true,
    insertAdditionalImplicitCasts = false,
) {
    private val enabled = context.config.enableIrInliner

    override fun lower(irModule: IrModuleFragment) {
        if (enabled) {
            super.lower(irModule)
        }
    }
}

class JvmInlineFunctionResolver(private val context: JvmBackendContext) : AbstractInlineFunctionResolver(InlineMode.ALL_INLINE_FUNCTIONS) {
    override fun needsInlining(symbol: IrFunctionSymbol): Boolean = symbol.owner.isInlineFunctionCall(context)
}
