/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance

object FirUpperBoundViolatedTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val container = context.containingElements.dropLast(1).lastOrNull()

        if (
            container is FirCallableDeclaration && typeRef.source?.kind is KtFakeSourceElementKind ||
            // `checkUpperBoundViolated()` will fully-expand and traverse the arguments on its own -
            // it must do so because bound violations within typealiases are use-site-dependent.
            // Kotlin doesn't support type parameter bounds for typealiases, meaning we can't prohibit
            // `typealias TA<F> = NotNullBox<F>` as there would be no workaround.
            container is FirTypeProjectionWithVariance || container is FirFunctionTypeParameter || container is FirFunctionTypeRef
        ) {
            return
        }

        checkUpperBoundViolated(typeRef, context.containingDeclarations.lastOrNull() is FirTypeAliasSymbol)
    }
}
