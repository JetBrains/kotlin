/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirInlineDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

abstract class FirInlineCheckerPlatformSpecificComponent : FirSessionComponent {
    open fun isGenerallyOk(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter): Boolean = true

    open val inlineVisitor get() = FirInlineDeclarationChecker::BasicInlineVisitor

    open fun checkSuspendFunctionalParameterWithDefaultValue(
        param: FirValueParameter,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
    }

    open fun checkFunctionalParametersWithInheritedDefaultValues(
        function: FirSimpleFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        overriddenSymbols: List<FirCallableSymbol<out FirCallableDeclaration>>,
    ) {
    }
}

val FirSession.inlineCheckerExtension by FirSession.nullableSessionComponentAccessor<FirInlineCheckerPlatformSpecificComponent>()
