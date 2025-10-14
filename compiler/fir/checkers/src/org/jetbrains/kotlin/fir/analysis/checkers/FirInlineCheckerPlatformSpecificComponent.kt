/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

abstract class FirInlineCheckerPlatformSpecificComponent : FirSessionComponent {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun isGenerallyOk(declaration: FirDeclaration, ): Boolean = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkSuspendFunctionalParameterWithDefaultValue(
        param: FirValueParameter,
    ) {
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkParametersWithInheritedDefaultValues(
        function: FirSimpleFunction,
        overriddenSymbols: List<FirCallableSymbol<FirCallableDeclaration>>,
    ) {
        val paramsWithDefaults = overriddenSymbols.flatMap {
            if (it !is FirFunctionSymbol<*>) return@flatMap emptyList()
            it.valueParameterSymbols.mapIndexedNotNull { idx, param ->
                idx.takeIf { param.hasDefaultValue }
            }
        }.toSet()
        val shouldReportError = shouldReportRegularOverridesWithDefaultParameters()
        function.valueParameters.forEachIndexed { idx, param ->
            if (param.defaultValue == null && paramsWithDefaults.contains(idx)) {
                reporter.reportOn(
                    param.source,
                    if (shouldReportError) FirErrors.NOT_YET_SUPPORTED_IN_INLINE else FirErrors.NOT_YET_SUPPORTED_IN_INLINE_WARNING,
                    "Functional parameters with inherited default values"
                )
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun shouldReportRegularOverridesWithDefaultParameters(): Boolean =
        LanguageFeature.ForbidOverriddenDefaultParametersInInline.isEnabled()

    companion object Default : FirInlineCheckerPlatformSpecificComponent()
}

val FirSession.inlineCheckerExtension: FirInlineCheckerPlatformSpecificComponent by FirSession.sessionComponentAccessor<FirInlineCheckerPlatformSpecificComponent>()
