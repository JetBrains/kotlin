/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.FirInlineCheckerPlatformSpecificComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class FirJvmInlineCheckerComponent : FirInlineCheckerPlatformSpecificComponent() {
    override fun isGenerallyOk(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        // local inline functions are prohibited
        return if (declaration.isLocalMember && context.containingDeclarations.lastOrNull() !is FirScript) {
            reporter.reportOn(declaration.source, FirErrors.NOT_YET_SUPPORTED_IN_INLINE, "Local inline functions", context)
            false
        } else {
            true
        }
    }

    override fun checkSuspendFunctionalParameterWithDefaultValue(
        param: FirValueParameter,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        reporter.reportOn(
            param.source,
            FirErrors.NOT_YET_SUPPORTED_IN_INLINE,
            "Suspend functional parameters with default values",
            context
        )
    }

    override fun checkFunctionalParametersWithInheritedDefaultValues(
        function: FirSimpleFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        overriddenSymbols: List<FirCallableSymbol<out FirCallableDeclaration>>,
    ) {
        val paramsWithDefaults = overriddenSymbols.flatMap {
            if (it !is FirFunctionSymbol<*>) return@flatMap emptyList<Int>()
            it.valueParameterSymbols.mapIndexedNotNull { idx, param ->
                idx.takeIf { param.hasDefaultValue }
            }
        }.toSet()
        function.valueParameters.forEachIndexed { idx, param ->
            if (param.defaultValue == null && paramsWithDefaults.contains(idx)) {
                reporter.reportOn(
                    param.source,
                    FirErrors.NOT_YET_SUPPORTED_IN_INLINE,
                    "Functional parameters with inherited default values",
                    context
                )
            }
        }
    }
}
