/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaredMemberScope
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.getClassRepresentativeForContextSensitiveResolution
import org.jetbrains.kotlin.fir.resolve.getParentChainForContextSensitiveResolution
import org.jetbrains.kotlin.fir.scopes.getClassifiers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name

object FirContextSensitiveResolutionAmbiguityChecker {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun check(contextualType: ConeKotlinType, resolvedSymbol: FirBasedSymbol<*>, name: Name, source: KtSourceElement?) {
        val chainToLookAt =
            contextualType.getClassRepresentativeForContextSensitiveResolution(context.session)
                ?.getParentChainForContextSensitiveResolution(context.session)
                ?.filter { it.isSealed }
                ?: return

        for (classToLookAt in chainToLookAt) {
            val problems = classToLookAt.declaredMemberScope().getClassifiers(name)
            if (problems.isEmpty()) continue
            if (resolvedSymbol !in problems) {
                reporter.reportOn(
                    source,
                    FirErrors.CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY,
                    listOf(resolvedSymbol) + problems
                )
            }
            return
        }
    }
}

object FirContextSensitiveResolutionAmbiguityCheckerForEqualities : FirEqualityOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirEqualityOperatorCall) {
        if (!LanguageFeature.ContextSensitiveResolutionUsingExpectedType.isEnabled()) return
        val rhs = expression.arguments[1]

        val resolvedSymbol = when (rhs) {
            is FirErrorResolvedQualifier -> null
            is FirResolvedQualifier if !rhs.isContextSensitiveResolved && rhs.explicitParent == null -> rhs.symbol
            is FirPropertyAccessExpression if rhs.explicitReceiver == null -> when (val callee = rhs.calleeReference) {
                is FirErrorNamedReference -> null
                is FirResolvedNamedReference if !callee.isContextSensitiveResolved -> callee.resolvedSymbol
                else -> null
            }
            else -> null
        } ?: return

        val name = when (resolvedSymbol) {
            is FirClassLikeSymbol<*> -> resolvedSymbol.name
            is FirCallableSymbol<*> -> resolvedSymbol.name
            else -> null
        } ?: return

        FirContextSensitiveResolutionAmbiguityChecker.check(
            contextualType = expression.arguments[0].resolvedType,
            resolvedSymbol = resolvedSymbol,
            name = name,
            source = rhs.source
        )
    }
}

object FirContextSensitiveResolutionAmbiguityCheckerForTypeOperators : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        if (!LanguageFeature.ContextSensitiveResolutionUsingExpectedType.isEnabled()) return
        val typeRef = expression.conversionTypeRef as? FirResolvedTypeRef ?: return
        if (typeRef.isContextSensitiveResolved || typeRef is FirErrorTypeRef) return

        val resolvedClass = typeRef.toClassLikeSymbol(context.session) ?: return
        val name = (typeRef.delegatedTypeRef as? FirUserTypeRef)?.qualifier?.singleOrNull()?.name ?: return

        FirContextSensitiveResolutionAmbiguityChecker.check(
            contextualType = expression.argument.resolvedType,
            resolvedSymbol = resolvedClass,
            name = name,
            source = typeRef.source
        )
    }
}

