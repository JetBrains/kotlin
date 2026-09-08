/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.hasExplicitReturnType
import org.jetbrains.kotlin.fir.analysis.js.checkers.expression.reportIfUnsafeSuspendFunctionalValue
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.types.coneType

object FirJsSuspendLambdaAsObjectPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        // Only a *declared* type is a boundary crossing; an inferred one isn't.
        if (!declaration.symbol.hasExplicitReturnType) return

        val initializer = declaration.initializer ?: return
        val propertyType = declaration.returnTypeRef.coneType
        reportIfUnsafeSuspendFunctionalValue(initializer, forcedType = propertyType)
    }
}
