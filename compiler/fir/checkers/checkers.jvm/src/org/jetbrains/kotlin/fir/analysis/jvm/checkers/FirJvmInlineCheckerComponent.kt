/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.FirInlineCheckerPlatformSpecificComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalDeclaredInBlock
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol

class FirJvmInlineCheckerComponent : FirInlineCheckerPlatformSpecificComponent() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun isGenerallyOk(declaration: FirFunction): Boolean {
        return if (declaration.isLocalDeclaredInBlock && context.containingDeclarations.lastOrNull() !is FirScriptSymbol) {
            reporter.reportOn(declaration.source, FirJvmErrors.NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION)
            false
        } else {
            true
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkSuspendFunctionalParameterWithDefaultValue(
        param: FirValueParameter,
    ) {
        reporter.reportOn(
            param.source,
            FirErrors.NOT_YET_SUPPORTED_IN_INLINE,
            "Suspend functional parameters with default values"
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun shouldReportRegularOverridesWithDefaultParameters(): Boolean =
        true
}
