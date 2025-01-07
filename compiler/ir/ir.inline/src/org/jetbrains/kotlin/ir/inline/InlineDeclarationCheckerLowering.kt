/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.checkers.IrInlineDeclarationChecker
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrVisitor

class InlineDeclarationCheckerLowering<Context : PreSerializationLoweringContext>(val context: Context) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            context.diagnosticReporter.deduplicating(),
            context.configuration.languageVersionSettings
        )

        irModule.runIrLevelCheckers(irDiagnosticReporter, ::IrInlineDeclarationChecker)
    }
}

fun IrModuleFragment.runIrLevelCheckers(
    diagnosticReporter: IrDiagnosticReporter,
    vararg checkers: (IrDiagnosticReporter) -> IrVisitor<*, Nothing?>,
) {
    for (checker in checkers) {
        accept(checker(diagnosticReporter), null)
    }
}
