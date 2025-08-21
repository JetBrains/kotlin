/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

abstract class FirInlineCheckerPlatformSpecificComponent : FirSessionComponent {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun isGenerallyOk(declaration: FirDeclaration, ): Boolean = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkSuspendFunctionalParameterWithDefaultValue(
        param: FirValueParameter,
    ) {
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    open fun checkFunctionalParametersWithInheritedDefaultValues(
        function: FirSimpleFunction,
        overriddenSymbols: List<FirCallableSymbol<FirCallableDeclaration>>,
    ) {
    }
}

val FirSession.inlineCheckerExtension: FirInlineCheckerPlatformSpecificComponent? by FirSession.nullableSessionComponentAccessor<FirInlineCheckerPlatformSpecificComponent>()
