/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

abstract class FirInlineCheckerPlatformSpecificComponent : FirComposableSessionComponent<FirInlineCheckerPlatformSpecificComponent> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun isGenerallyOk(declaration: FirDeclaration, ): Boolean = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkSuspendFunctionalParameterWithDefaultValue(
        param: FirValueParameter,
    ) {
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkFunctionalParametersWithInheritedDefaultValues(
        function: FirNamedFunction,
        overriddenSymbols: List<FirCallableSymbol<FirCallableDeclaration>>,
    ) {
        val paramsWithDefaults = overriddenSymbols.flatMap {
            if (it !is FirFunctionSymbol<*>) return@flatMap emptyList()
            it.valueParameterSymbols.mapIndexedNotNull { idx, param ->
                idx.takeIf { param.hasDefaultValue }
            }
        }.toSet()
        function.valueParameters.forEachIndexed { idx, param ->
            if (param.defaultValue == null && paramsWithDefaults.contains(idx)) {
                reporter.reportOn(
                    param.source,
                    FirErrors.NOT_YET_SUPPORTED_IN_INLINE,
                    "Functional parameters with inherited default values"
                )
            }
        }
    }

    companion object Default : FirInlineCheckerPlatformSpecificComponent()

    class Composed(
        override val components: List<FirInlineCheckerPlatformSpecificComponent>
    ) : FirInlineCheckerPlatformSpecificComponent(), FirComposableSessionComponent.Composed<FirInlineCheckerPlatformSpecificComponent> {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun isGenerallyOk(declaration: FirDeclaration): Boolean {
            return components.all { it.isGenerallyOk(declaration) }
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun checkSuspendFunctionalParameterWithDefaultValue(param: FirValueParameter) {
            components.forEach { it.checkSuspendFunctionalParameterWithDefaultValue(param) }
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun checkFunctionalParametersWithInheritedDefaultValues(
            function: FirNamedFunction,
            overriddenSymbols: List<FirCallableSymbol<FirCallableDeclaration>>,
        ) {
            components.forEach { it.checkFunctionalParametersWithInheritedDefaultValues(function, overriddenSymbols) }
        }
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirInlineCheckerPlatformSpecificComponent>): Composed {
        return Composed(components)
    }
}

val FirSession.inlineCheckerExtension: FirInlineCheckerPlatformSpecificComponent by FirSession.sessionComponentAccessor<FirInlineCheckerPlatformSpecificComponent>()
