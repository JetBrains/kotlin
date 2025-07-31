/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
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
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtNameReferenceExpressionElementType

object FirContextSensitiveResolutionAmbiguityChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        if (!LanguageFeature.ContextSensitiveResolutionUsingExpectedType.isEnabled()) return
        when (expression) {
            is FirEqualityOperatorCall -> checkEquality(expression)
            is FirTypeOperatorCall -> checkTypeOperator(expression)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkEquality(equalityOperatorCall: FirEqualityOperatorCall) {
        val rhs = equalityOperatorCall.arguments[1]
        val name = rhs.getReferencedName() ?: return

        val resolvedSymbol = when (rhs) {
            is FirErrorResolvedQualifier -> null
            is FirResolvedQualifier -> rhs.symbol
            is FirPropertyAccessExpression -> when (val callee = rhs.calleeReference) {
                is FirErrorNamedReference -> null
                is FirResolvedNamedReference if !callee.isContextSensitiveResolved -> callee.resolvedSymbol
                else -> null
            }
            else -> null
        } ?: return

        check(
            contextualType = equalityOperatorCall.argument.resolvedType,
            resolvedSymbol = resolvedSymbol,
            name = name,
            source = rhs.source
        )
    }

    fun FirExpression.getReferencedName(): Name? = when (val source = source) {
        is KtRealPsiSourceElement -> (source.psi as? KtNameReferenceExpression)?.getReferencedNameAsName()
        is KtLightSourceElement if source.elementType is KtNameReferenceExpressionElementType ->
            Name.identifier(source.lighterASTNode.toString())
        else -> null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkTypeOperator(typeOperatorCall: FirTypeOperatorCall) {
        val typeRef = typeOperatorCall.conversionTypeRef as? FirResolvedTypeRef ?: return
        if (typeRef is FirErrorTypeRef) return

        val resolvedClass = typeRef.toClassLikeSymbol(context.session) ?: return

        val userTypeRef = typeRef.delegatedTypeRef as? FirUserTypeRef ?: return
        val qualifier = userTypeRef.qualifier.singleOrNull() ?: return

        check(
            contextualType = typeOperatorCall.argument.resolvedType,
            resolvedSymbol = resolvedClass,
            name = qualifier.name,
            source = typeRef.source
        )
    }

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
            if (problems.none { it == resolvedSymbol }) {
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