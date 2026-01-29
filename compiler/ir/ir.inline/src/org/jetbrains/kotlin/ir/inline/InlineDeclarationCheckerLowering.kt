/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.checkers.IrInlineDeclarationChecker

// When invoked without prior private inliner, IrInlineDeclarationChecker won't report cascading diagnostics.
@PhasePrerequisites(FunctionInlining::class) // only private inlining is required
class InlineDeclarationCheckerLowering<Context : PreSerializationLoweringContext>(val context: Context) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        irModule.accept(IrInlineDeclarationChecker(context), null)
    }
}
