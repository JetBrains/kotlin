/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.allReceiverExpressions
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.text

object FirReifiedChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val calleeReference = expression.calleeReference
        val typeArguments = expression.typeArguments
        val callableSymbol = calleeReference.toResolvedCallableSymbol()
        val typeParameters = callableSymbol?.typeParameterSymbols ?: return

        val count = minOf(typeArguments.size, typeParameters.size)
        val varargParameter = (callableSymbol as? FirFunctionSymbol<*>)?.valueParameterSymbols?.singleOrNull { it.isVararg }
        val varargElementType = varargParameter?.resolvedReturnType?.arrayElementType()
        val varargTypeParameter = (varargElementType?.unwrapToSimpleTypeUsingLowerBound() as? ConeTypeParameterType?)?.lookupTag?.typeParameterSymbol
        for (index in 0 until count) {
            val typeArgumentProjection = typeArguments.elementAt(index)
            val source = typeArgumentProjection.source ?: calleeReference.source ?: continue

            val typeArgument = typeArgumentProjection.toConeTypeProjection().type?.fullyExpandedType()?.lowerBoundIfFlexible() ?: continue
            val typeParameter = typeParameters[index]

            val isExplicit = typeArgumentProjection.source?.kind == KtRealSourceElementKind
            val isPlaceHolder = isExplicit && typeArgumentProjection.source.text == "_"
            val isInferred = !isExplicit || isPlaceHolder

            if (typeParameter.isReifiedTypeParameterOrFromKotlinArray()) {
                checkArgumentAndReport(
                    typeArgument,
                    typeParameter,
                    source,
                    isExplicit = isExplicit,
                    isArray = false,
                    isPlaceHolder = isPlaceHolder,
                    fullyExpandedType = typeArgument,
                )
            } else if (
                varargTypeParameter == typeParameter && typeArgument.isTypeVisibilityBroken(checkTypeArguments = false) && isInferred
            ) {
                reporter.reportOn(
                    source, FirErrors.INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT, typeParameter, typeArgument, varargParameter
                )
            }
        }
        validateReturnTypeVisibility(expression, calleeReference, callableSymbol)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun validateReturnTypeVisibility(
        expression: FirQualifiedAccessExpression,
        calleeReference: FirReference,
        callableSymbol: FirCallableSymbol<*>,
    ) {
        val returnType = expression.resolvedType
        if (calleeReference is FirResolvedErrorReference) return
        if (expression.typeArguments.any { it is FirTypeProjectionWithVariance && it.typeRef is FirErrorTypeRef }) return
        if (expression !is FirFunctionCall) return
        val hasNestedVisibilityErrorsInParameters = (expression.allReceiverExpressions + expression.arguments)
            .any { it.resolvedType is ConeErrorType || it.resolvedType.isTypeVisibilityBroken(checkTypeArguments = true) }
        if (hasNestedVisibilityErrorsInParameters) return
        if (returnType.isTypeVisibilityBroken(checkTypeArguments = true)) {
            reporter.reportOn(expression.source, FirErrors.INFERRED_INVISIBLE_RETURN_TYPE, callableSymbol, returnType)
        }
    }

    private fun FirTypeParameterSymbol.isReifiedTypeParameterOrFromKotlinArray(): Boolean {
        val containingDeclaration = containingDeclarationSymbol
        return isReified ||
                containingDeclaration is FirRegularClassSymbol && containingDeclaration.classId == StandardClassIds.Array
    }

    private fun ConeKotlinType.cannotBeReified(languageVersionSettings: LanguageVersionSettings): Boolean = when (this) {
        is ConeCapturedType -> true
        is ConeDynamicType -> true
        else -> unsupportedKindOfNothingAsReifiedOrInArray(languageVersionSettings) != null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkArgumentAndReport(
        typeArgument: ConeKotlinType,
        typeParameter: FirTypeParameterSymbol,
        source: KtSourceElement,
        isExplicit: Boolean,
        isArray: Boolean,
        isPlaceHolder: Boolean,
        fullyExpandedType: ConeKotlinType = typeArgument.fullyExpandedType(),
    ) {
        if (fullyExpandedType.classId == StandardClassIds.Array) {
            // Type aliases can transform type arguments arbitrarily (drop, nest, etc...).
            // Therefore, we check the arguments of the expanded type, not the ones that went into the type alias.
            fullyExpandedType.typeArguments.forEach {
                if (it is ConeKotlinType) checkArgumentAndReport(
                    it,
                    typeParameter,
                    source,
                    isExplicit,
                    isArray = true,
                    isPlaceHolder = isPlaceHolder,
                )
            }
            return
        }
        if (fullyExpandedType.isTypeVisibilityBroken(checkTypeArguments = false) && (!isExplicit || isPlaceHolder)) {
            reporter.reportOn(source, FirErrors.INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT, typeParameter, fullyExpandedType)
        }

        if (typeArgument is ConeTypeParameterType) {
            val symbol = typeArgument.lookupTag.typeParameterSymbol
            if (!symbol.isReified) {
                reporter.reportOn(
                    source,
                    if (isArray) FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR else FirErrors.TYPE_PARAMETER_AS_REIFIED,
                    symbol,
                )
            }
        } else if (typeArgument is ConeDefinitelyNotNullType && isExplicit) {
            // We sometimes infer type arguments to DNN types, which seems to be ok. Only report explicit DNN types written by user.
            reporter.reportOn(source, FirErrors.DEFINITELY_NON_NULLABLE_AS_REIFIED)
        } else if (typeArgument.cannotBeReified(context.languageVersionSettings)) {
            reporter.reportOn(source, FirErrors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, typeArgument)
        } else if (typeArgument is ConeIntersectionType) {
            reporter.reportOn(source, FirErrors.TYPE_INTERSECTION_AS_REIFIED, typeParameter, typeArgument.intersectedTypes)
        }
    }

}
