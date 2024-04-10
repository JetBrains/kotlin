/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature.ForbidLambdaParameterWithMissingDependencyType
import org.jetbrains.kotlin.config.LanguageFeature.ForbidUsingExpressionTypesWithInaccessibleContent
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirMissingDependencyClassProxy.MissingTypeOrigin.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithSingleCandidate
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name

/**
 * @see org.jetbrains.kotlin.resolve.checkers.MissingDependencyClassChecker
 */
object FirMissingDependencyClassChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common), FirMissingDependencyClassProxy {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeReference = expression.calleeReference
        val missingTypes = mutableSetOf<ConeKotlinType>()
        val missingTypesFromExpression = mutableSetOf<ConeKotlinType>()
        val containingElements = context.containingElements
        if (!calleeReference.isError()) {
            expression.resolvedType.forEachType {
                if (it is ConeErrorType) {
                    // To report error instead of warning in a known corner case (KT-66356)
                    val partOfErroneousOuterCall =
                        containingElements.any { it is FirFunctionCall && it.calleeReference is FirResolvedErrorReference } &&
                                !context.session.languageVersionSettings.supportsFeature(ForbidUsingExpressionTypesWithInaccessibleContent)
                    considerType(
                        type = it,
                        missingTypes = if (partOfErroneousOuterCall) missingTypes else missingTypesFromExpression,
                        context
                    )
                }
            }
        }


        // To replicate K1 behavior, MISSING_DEPENDENCY_CLASS errors should still be reported on error references with a single candidate.
        // All other callee errors should skip reporting of MISSING_DEPENDENCY_CLASS.
        if (calleeReference.isError() && calleeReference.diagnostic !is ConeDiagnosticWithSingleCandidate &&
            missingTypesFromExpression.isEmpty()
        ) return

        val symbol = calleeReference.toResolvedCallableSymbol() ?: return
        considerType(symbol.resolvedReturnTypeRef.coneType, missingTypes, context)
        symbol.resolvedReceiverTypeRef?.coneType?.let {
            considerType(it, missingTypes, context)
        }
        (symbol as? FirFunctionSymbol<*>)?.valueParameterSymbols?.forEach {
            considerType(it.resolvedReturnTypeRef.coneType, missingTypes, context)
        }
        reportMissingTypes(
            expression.source, missingTypes, context, reporter,
            missingTypeOrigin = Other
        )
        if (missingTypes.isEmpty()) {
            reportMissingTypes(
                expression.source, missingTypesFromExpression, context, reporter,
                missingTypeOrigin = Expression
            )
        }
    }
}

internal interface FirMissingDependencyClassProxy {
    fun ConeKotlinType.forEachClassLikeType(action: (ConeClassLikeType) -> Unit) {
        when (this) {
            is ConeFlexibleType -> {
                lowerBound.forEachClassLikeType(action)
                upperBound.forEachClassLikeType(action)
            }

            is ConeDefinitelyNotNullType -> original.forEachClassLikeType(action)
            is ConeIntersectionType -> intersectedTypes.forEach { it.forEachClassLikeType(action) }
            is ConeClassLikeType -> action(this)
            else -> {} // Ignore all type parameters.
        }
    }

    fun considerType(type: ConeKotlinType, missingTypes: MutableSet<ConeKotlinType>, context: CheckerContext) {
        var hasError = false
        var hasMissingClass = false
        type.forEachClassLikeType {
            when (it) {
                is ConeErrorType -> {
                    val delegatedType = it.delegatedType
                    if (delegatedType == null) {
                        hasError = true
                    } else {
                        considerType(delegatedType, missingTypes, context)
                    }
                }
                else -> hasMissingClass = hasMissingClass || it.lookupTag.toSymbol(context.session) == null
            }
        }

        if (hasMissingClass && !hasError) {
            val reportedType = type.withNullability(ConeNullability.NOT_NULL, context.session.typeContext)
            missingTypes.add(reportedType)
        }
    }

    sealed class MissingTypeOrigin {
        class LambdaParameter(val name: Name) : MissingTypeOrigin()
        object LambdaReceiver : MissingTypeOrigin()
        object Expression : MissingTypeOrigin()
        object Other : MissingTypeOrigin()
    }

    fun reportMissingTypes(
        source: KtSourceElement?,
        missingTypes: MutableSet<ConeKotlinType>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        missingTypeOrigin: MissingTypeOrigin
    ) {
        val reported = mutableSetOf<ConeKotlinType>()
        val languageVersionSettings = context.session.languageVersionSettings
        for (missingType in missingTypes) {
            val withoutArguments = missingType.withArguments(emptyArray())
            if (withoutArguments in reported) continue
            // We report an error MISSING_DEPENDENCY_CLASS generally,
            // but report a deprecation warning in two corner cases instead to avoid breaking code immediately
            when {
                missingTypeOrigin is LambdaParameter && missingType.typeArguments.isEmpty() &&
                        !languageVersionSettings.supportsFeature(ForbidLambdaParameterWithMissingDependencyType) -> {
                    reporter.reportOn(
                        source, FirErrors.MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER, withoutArguments, missingTypeOrigin.name, context
                    )
                }
                missingTypeOrigin is LambdaReceiver && missingType.typeArguments.isEmpty() &&
                        !languageVersionSettings.supportsFeature(ForbidLambdaParameterWithMissingDependencyType) -> {
                    reporter.reportOn(source, FirErrors.MISSING_DEPENDENCY_CLASS_IN_LAMBDA_RECEIVER, withoutArguments, context)
                }
                missingTypeOrigin is Expression &&
                        !languageVersionSettings.supportsFeature(ForbidUsingExpressionTypesWithInaccessibleContent) -> {
                    reporter.reportOn(source, FirErrors.MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE, withoutArguments, context)
                }
                else -> {
                    reporter.reportOn(source, FirErrors.MISSING_DEPENDENCY_CLASS, withoutArguments, context)
                }
            }
            reported += withoutArguments
        }
    }
}
