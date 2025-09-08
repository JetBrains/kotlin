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
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithSingleCandidate
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name

object FirMissingDependencyClassChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common), FirMissingDependencyClassProxy {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val calleeReference = expression.calleeReference
        val missingTypes = mutableSetOf<ConeClassLikeType>()
        val missingTypesFromExpression = mutableSetOf<ConeClassLikeType>()
        val containingElements = context.containingElements
        if (!calleeReference.isError()) {
            expression.resolvedType.forEachType { type ->
                // To report error instead of warning in a known corner case (KT-66356)
                val partOfErroneousOuterCall =
                    type is ConeErrorType && containingElements.any {
                        (it as? FirFunctionCall)?.calleeReference is FirResolvedErrorReference
                    } && !ForbidUsingExpressionTypesWithInaccessibleContent.isEnabled()
                considerType(
                    type = type,
                    missingTypes = if (partOfErroneousOuterCall) missingTypes else missingTypesFromExpression
                )
            }
        }


        // To replicate K1 behavior, MISSING_DEPENDENCY_CLASS errors should still be reported on error references with a single candidate.
        // All other callee errors should skip reporting of MISSING_DEPENDENCY_CLASS.
        if (calleeReference.isError() && calleeReference.diagnostic !is ConeDiagnosticWithSingleCandidate &&
            missingTypesFromExpression.isEmpty()
        ) return

        val symbol = calleeReference.toResolvedCallableSymbol() ?: return
        considerType(symbol.resolvedReturnTypeRef.coneType, missingTypes)
        symbol.resolvedReceiverType?.let { type ->
            considerType(type, missingTypes)
            type.forEachType {
                considerType(it, if (type.isArrayTypeOrNullableArrayType) missingTypes else missingTypesFromExpression)
            }
        }
        if (expression is FirFunctionCall) {
            val argumentList = expression.argumentList as? FirResolvedArgumentList
            val visitedParameterSymbols = hashSetOf<FirValueParameterSymbol>()
            argumentList?.mapping?.forEach { (_, parameter) ->
                visitedParameterSymbols += parameter.symbol
                val type = parameter.returnTypeRef.coneType
                considerType(type, missingTypes)
                type.forEachType {
                    considerType(it, if (type.isArrayTypeOrNullableArrayType) missingTypes else missingTypesFromExpression)
                }
            }
            (symbol as? FirFunctionSymbol<*>)?.valueParameterSymbols?.forEach { parameterSymbol ->
                if (parameterSymbol in visitedParameterSymbols) return@forEach
                val type = parameterSymbol.resolvedReturnTypeRef.coneType
                if (type.isArrayTypeOrNullableArrayType) {
                    type.forEachType {
                        considerType(it, missingTypes)
                    }
                }
            }
        }
        reportMissingTypes(
            expression.source, missingTypes,
            missingTypeOrigin = Other
        )
        if (missingTypes.isEmpty()) {
            reportMissingTypes(
                expression.source, missingTypesFromExpression,
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

    context(context: CheckerContext)
    fun considerType(type: ConeKotlinType, missingTypes: MutableSet<ConeClassLikeType>) {
        var hasError = false
        var missingClasses: MutableSet<ConeClassLikeType>? = null
        type.forEachClassLikeType { type ->
            if (type is ConeErrorType) {
                val delegatedType = type.delegatedType
                if (delegatedType == null) {
                    hasError = true
                } else {
                    considerType(delegatedType, missingTypes)
                }
            } else if (type.lookupTag.toSymbol() == null) {
                (missingClasses ?: mutableSetOf<ConeClassLikeType>().also { missingClasses = it }) +=
                    type.lookupTag.constructClassType()
            }
        }

        if (!hasError) {
            missingClasses?.let { missingTypes.addAll(it) }
        }
    }

    sealed class MissingTypeOrigin {
        class LambdaParameter(val name: Name) : MissingTypeOrigin()
        object LambdaReceiver : MissingTypeOrigin()
        object Expression : MissingTypeOrigin()
        object Other : MissingTypeOrigin()
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun reportMissingTypes(
        source: KtSourceElement?,
        missingTypes: MutableSet<ConeClassLikeType>,
        missingTypeOrigin: MissingTypeOrigin
    ) {
        val languageVersionSettings = context.session.languageVersionSettings
        for (missingType in missingTypes) {
            // We report an error MISSING_DEPENDENCY_CLASS generally,
            // but report a deprecation warning in two corner cases instead to avoid breaking code immediately
            when (missingTypeOrigin) {
                is LambdaParameter if missingType.typeArguments.isEmpty() &&
                        !languageVersionSettings.supportsFeature(ForbidLambdaParameterWithMissingDependencyType) -> {
                    reporter.reportOn(
                        source, FirErrors.MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER, missingType, missingTypeOrigin.name
                    )
                }
                is LambdaReceiver if missingType.typeArguments.isEmpty() &&
                        !languageVersionSettings.supportsFeature(ForbidLambdaParameterWithMissingDependencyType) -> {
                    reporter.reportOn(source, FirErrors.MISSING_DEPENDENCY_CLASS_IN_LAMBDA_RECEIVER, missingType)
                }
                is Expression if !languageVersionSettings.supportsFeature(ForbidUsingExpressionTypesWithInaccessibleContent) -> {
                    reporter.reportOn(source, FirErrors.MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE, missingType)
                }
                else -> {
                    reporter.reportOn(source, FirErrors.MISSING_DEPENDENCY_CLASS, missingType)
                }
            }
        }
    }
}
