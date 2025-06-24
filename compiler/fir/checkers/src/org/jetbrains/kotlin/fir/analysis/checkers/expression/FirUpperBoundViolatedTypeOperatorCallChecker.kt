/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

object FirUpperBoundViolatedTypeOperatorCallChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        val typeRef = expression.conversionTypeRef as? FirResolvedTypeRef ?: return
        val isUserTypeWithoutArguments = (typeRef.delegatedTypeRef as? FirUserTypeRef)?.qualifier?.lastOrNull()
            .let { it != null && it.typeArgumentList.typeArguments.isEmpty() }

        // Otherwise, we'd fail on bare casts like `it is FirClassSymbol` for `it: FirBasedSymbol<FirDeclaration>`
        // because the compiler infers `FirClassSymbol<FirDeclaration>` for the RHS.
        if (!isUserTypeWithoutArguments) {
            checkUpperBoundViolated(expression.conversionTypeRef, isInsideTypeOperatorOrParameterBounds = true)
        }
    }
}
