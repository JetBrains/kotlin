/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractTypeRefAndSourceFromTypeArgument
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirUpperBoundViolatedChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleeFir = expression.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol
            ?.fir.safeAs<FirTypeParameterRefsOwner>()
            ?: return

        val coneType = expression.typeRef.coneType
        if (coneType is ConeClassLikeType) {
            analyzeTypeParameters(
                coneType,
                expression.typeRef,
                calleeFir.typeParameters.map { it.symbol },
                expression.typeArguments,
                context,
                reporter
            )
        }
    }

    /**
     * Recursively analyzes type parameters and reports the diagnostic on the given source calculated using typeRef
     * Returns true if an error occurred
     */
    private fun analyzeTypeParameters(
        type: ConeClassLikeType,
        typeRef: FirTypeRef?,
        typeParameters: List<FirTypeParameterSymbol>?,
        typeArguments: List<FirTypeProjection>?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        fun getTypeArgument(index: Int): Any {
            return typeArguments?.elementAt(index) ?: type.typeArguments[index]
        }

        val typeArgumentsCount = typeArguments?.size ?: type.typeArguments.size
        if (typeArgumentsCount == 0) {
            return
        }

        val typeParameterSymbols = if (typeParameters != null) {
            typeParameters
        } else {
            val prototypeClass = type.lookupTag.toSymbol(context.session)
                ?.fir.safeAs<FirRegularClass>()
                ?: return

            prototypeClass.typeParameters.map { it.symbol }
        }

        if (typeParameterSymbols.isEmpty()) {
            return
        }

        val count = minOf(typeParameterSymbols.size, typeArgumentsCount)
        val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()

        for (index in 0 until count) {
            val typeArgument = getTypeArgument(index)
            val typeParameterSymbol = typeParameterSymbols[index]

            if (typeArgument is FirTypeProjectionWithVariance) {
                substitution[typeParameterSymbol] = typeArgument.typeRef.coneType
            } else if (typeArgument is ConeClassLikeType) {
                substitution[typeParameterSymbol] = typeArgument.type
            }
        }

        val substitutor = substitutorByMap(substitution, context.session)
        val typeSystemContext = context.session.typeContext

        for (index in 0 until count) {
            var typeArgument: ConeClassLikeType? = null
            var typeArgumentTypeRef: FirTypeRef? = null
            var typeArgumentSource: FirSourceElement? = null

            if (typeArguments != null) {
                val localTypeArgument = typeArguments[index]
                if (localTypeArgument is FirTypeProjectionWithVariance) {
                    typeArgumentTypeRef = localTypeArgument.typeRef
                    typeArgument = typeArgumentTypeRef.coneType as? ConeClassLikeType
                    typeArgumentSource = localTypeArgument.source
                }
            } else {
                typeArgument = type.typeArguments[index] as? ConeClassLikeType
                val argTypeRefSource = extractTypeRefAndSourceFromTypeArgument(typeRef, index)
                typeArgumentTypeRef = argTypeRefSource?.first
                typeArgumentSource = argTypeRefSource?.second
            }

            if (typeArgument != null && typeArgumentSource != null) {
                val upperBound = getSubstitutedUpperBound(typeParameterSymbols[index], substitutor, typeSystemContext)
                if (upperBound != null && !satisfiesBounds(upperBound, typeArgument.type, typeSystemContext)) {
                    reporter.reportOn(typeArgumentSource, FirErrors.UPPER_BOUND_VIOLATED, upperBound, context)
                } else {
                    analyzeTypeParameters(typeArgument, typeArgumentTypeRef, null, null, context, reporter)
                }
            }
        }
    }

    private fun getSubstitutedUpperBound(
        prototypeSymbol: FirTypeParameterSymbol,
        substitutor: ConeSubstitutor,
        typeSystemContext: ConeTypeContext
    ): ConeKotlinType? {
        val intersection = typeSystemContext.intersectTypes(
            prototypeSymbol.fir.bounds.map { it.coneType }
        ).safeAs<ConeKotlinType>() ?: return null

        return substitutor.substituteOrSelf(intersection)
    }

    private fun satisfiesBounds(upperBound: ConeKotlinType, target: ConeKotlinType, typeSystemContext: ConeTypeContext): Boolean {
        return AbstractTypeChecker.isSubtypeOf(typeSystemContext, target, upperBound, stubTypesEqualToAnything = false)
    }
}
