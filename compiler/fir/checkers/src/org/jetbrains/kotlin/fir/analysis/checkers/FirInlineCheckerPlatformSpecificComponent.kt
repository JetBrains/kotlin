/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

abstract class FirInlineCheckerPlatformSpecificComponent : FirComposableSessionComponent<FirInlineCheckerPlatformSpecificComponent> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun isGenerallyOk(declaration: FirFunction): Boolean = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkSuspendFunctionalParameterWithDefaultValue(
        param: FirValueParameter,
    ) {
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkParametersWithInheritedDefaultValues(
        function: FirNamedFunction,
        overriddenSymbols: List<FirCallableSymbol<FirCallableDeclaration>>,
    ) {
        val paramHasDefault = BooleanArray(function.valueParameters.size)
        overriddenSymbols.forEach {
            if (it !is FirFunctionSymbol<*>) return@forEach
            it.valueParameterSymbols.forEachIndexed { idx, param ->
                if (param.hasDefaultValue && idx < paramHasDefault.size) {
                    paramHasDefault[idx] = true
                }
            }
        }
        val shouldReportError = shouldReportRegularOverridesWithDefaultParameters()
        function.valueParameters.forEachIndexed { idx, param ->
            if (param.defaultValue == null && paramHasDefault[idx]) {
                if (shouldReportError) {
                    reporter.reportOn(
                        param.source,
                        FirErrors.NOT_YET_SUPPORTED_IN_INLINE,
                        "Parameters with inherited default values"
                    )
                } else {
                    reporter.reportOn(
                        param.source,
                        FirErrors.NOT_YET_SUPPORTED_IN_INLINE_WARNING
                    )
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun shouldReportRegularOverridesWithDefaultParameters(): Boolean =
        LanguageFeature.ForbidOverriddenDefaultParametersInInline.isEnabled()

    /**
     * Default component for inline checking, currently used for all non-JVM platforms
     *
     * Any other component overrides this one: either JVM- or another platform-specific (if any of such appears later).
     * See [Composed.nonDuplicatingComponents]
     */
    object NonJvmDefault : FirInlineCheckerPlatformSpecificComponent()

    class Composed(
        override val components: List<FirInlineCheckerPlatformSpecificComponent>
    ) : FirInlineCheckerPlatformSpecificComponent(), FirComposableSessionComponent.Composed<FirInlineCheckerPlatformSpecificComponent> {
        private val nonDuplicatingComponents: List<FirInlineCheckerPlatformSpecificComponent> =
            if (components.size == 1) components else components.filter { it !== NonJvmDefault }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun isGenerallyOk(declaration: FirFunction): Boolean {
            return nonDuplicatingComponents.all { it.isGenerallyOk(declaration) }
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun checkSuspendFunctionalParameterWithDefaultValue(param: FirValueParameter) {
            nonDuplicatingComponents.forEach { it.checkSuspendFunctionalParameterWithDefaultValue(param) }
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun checkParametersWithInheritedDefaultValues(
            function: FirNamedFunction,
            overriddenSymbols: List<FirCallableSymbol<FirCallableDeclaration>>,
        ) {
            nonDuplicatingComponents.forEach { it.checkParametersWithInheritedDefaultValues(function, overriddenSymbols) }
        }
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirInlineCheckerPlatformSpecificComponent>): Composed {
        return Composed(components)
    }
}

val FirSession.inlineCheckerExtension: FirInlineCheckerPlatformSpecificComponent by FirSession.sessionComponentAccessor<FirInlineCheckerPlatformSpecificComponent>()
