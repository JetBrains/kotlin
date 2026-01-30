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
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

object FirUpperBoundViolatedTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val container = context.parentElement

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

        val isUserTypeWithoutArguments = (typeRef.delegatedTypeRef as? FirUserTypeRef)?.qualifier?.lastOrNull()
            .let { it != null && it.typeArgumentList.typeArguments.isEmpty() }
        val isInTypeOperatorCall = container is FirTypeOperatorCall
        val isBareType = isInTypeOperatorCall && isUserTypeWithoutArguments
        val isInParameterBoundsOfNonClass = container is FirTypeParameter && context.secondParentElement !is FirClass

        // Otherwise, we'd fail on bare casts like `it is FirClassSymbol` for `it: FirBasedSymbol<FirDeclaration>`
        // because the compiler infers `FirClassSymbol<FirDeclaration>` for the RHS.
        if (isBareType) {
            return
        }

        checkUpperBoundViolated(
            typeRef,
            isIgnoreTypeParameters = context.containingDeclarations.lastOrNull() is FirTypeAliasSymbol,
            isInsideTypeOperatorOrParameterBounds = isInTypeOperatorCall || isInParameterBoundsOfNonClass,
        )
    }

    private val CheckerContext.parentElement
        get() = containingElements.let { it.elementAtOrNull(it.size - 2) }

    private val CheckerContext.secondParentElement
        get() = containingElements.let { it.elementAtOrNull(it.size - 3) }
}