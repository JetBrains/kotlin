/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaredMemberScope
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.staticScope
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.FirResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.resolve.getParentChainForContextSensitiveResolutionOfExpressions
import org.jetbrains.kotlin.fir.resolve.getParentChainForContextSensitiveResolutionOfTypes
import org.jetbrains.kotlin.fir.scopes.getClassifiers
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name

object FirContextSensitiveResolutionAmbiguityCheckerForEqualities : FirEqualityOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirEqualityOperatorCall) {
        if (!LanguageFeature.ContextSensitiveResolutionUsingExpectedType.isEnabled()) return
        val rhs = expression.arguments[1]

        val resolvedSymbol = when (rhs) {
            is FirErrorResolvedQualifier -> null
            is FirResolvedQualifier if rhs.resolvedSymbolOrigin.shouldWarn && rhs.explicitParent == null -> rhs.symbol
            is FirPropertyAccessExpression if rhs.explicitReceiver == null -> when (val callee = rhs.calleeReference) {
                is FirErrorNamedReference -> null
                is FirResolvedNamedReference if callee.resolvedSymbolOrigin.shouldWarn -> callee.resolvedSymbol
                else -> null
            }
            else -> null
        } ?: return

        val name = when (resolvedSymbol) {
            is FirClassLikeSymbol<*> -> resolvedSymbol.name
            is FirCallableSymbol<*> -> resolvedSymbol.name
            else -> null
        } ?: return

        for (classToLookAt in expression.arguments[0].resolvedType.getParentChainForContextSensitiveResolutionOfExpressions(context.session)) {
            val contextSensitiveCandidates = classToLookAt.contextSensitiveCandidates(name)
            if (contextSensitiveCandidates.isEmpty()) continue
            if (resolvedSymbol !in contextSensitiveCandidates) {
                reporter.reportOn(
                    rhs.source,
                    FirErrors.CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY,
                    resolvedSymbol,
                    contextSensitiveCandidates
                )
            }
            // at this point we've either found a clash (and reported an error)
            // or the resolved symbol coincides with the context-sensitive one
            return
        }
    }

    context(context: CheckerContext)
    fun FirRegularClassSymbol.contextSensitiveCandidates(name: Name): List<FirBasedSymbol<*>> = buildList {
        if (isSealed) {
            declaredMemberScope().processClassifiersByName(name, this::add)
        }
        staticScope(context.sessionHolder)?.processPropertiesByName(name) {
            if (it.isNoArgumentProperty) add(it)
        }
        resolvedCompanionObjectSymbol?.declaredMemberScope()?.processPropertiesByName(name) {
            if (it.isNoArgumentProperty) add(it)
        }
    }
}

object FirContextSensitiveResolutionAmbiguityCheckerForTypeOperators : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        if (!LanguageFeature.ContextSensitiveResolutionUsingExpectedType.isEnabled()) return
        val typeRef = expression.conversionTypeRef as? FirResolvedTypeRef ?: return
        if (!typeRef.resolvedSymbolOrigin.shouldWarn || typeRef is FirErrorTypeRef) return

        val resolvedClass = typeRef.toClassLikeSymbol(context.session) ?: return
        val name = (typeRef.delegatedTypeRef as? FirUserTypeRef)?.qualifier?.singleOrNull()?.name ?: return

        for (classToLookAt in expression.argument.resolvedType.getParentChainForContextSensitiveResolutionOfTypes(context.session)) {
            val nestedClassifierSymbols = classToLookAt.declaredMemberScope().getClassifiers(name)
            if (nestedClassifierSymbols.isEmpty()) continue
            if (resolvedClass !in nestedClassifierSymbols) {
                reporter.reportOn(
                    typeRef.source,
                    FirErrors.CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY,
                    resolvedClass,
                    nestedClassifierSymbols,
                )
            }
            // at this point we've either found a clash (and reported an error)
            // or the resolved symbol coincides with the context-sensitive one
            return
        }
    }
}

private val FirResolvedSymbolOrigin?.shouldWarn: Boolean
    get() = this != FirResolvedSymbolOrigin.ContextSensitive

private val FirVariableSymbol<*>.isNoArgumentProperty: Boolean
    get() = !isExtension && !hasContextParameters
