/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirUpperBoundViolatedChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleeFir = expression.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol
            ?.fir.safeAs<FirTypeParameterRefsOwner>()
            ?: return

        val count = minOf(calleeFir.typeParameters.size, expression.typeArguments.size)

        if (count == 0) {
            return
        }

        val parameterPairs = mutableMapOf<FirTypeParameterSymbol, FirTypeRef>()

        for (it in 0 until count) {
            expression.typeArguments[it].safeAs<FirTypeProjectionWithVariance>()
                ?.typeRef
                ?.let { that ->
                    if (that !is FirErrorTypeRef) {
                        parameterPairs[calleeFir.typeParameters[it].symbol] = that
                    } else {
                        return
                    }
                }
        }

        // we substitute actual values to the
        // type parameters from the declaration
        val substitutor = substitutorByMap(
            parameterPairs.mapValues { it.value.coneType },
            context.session
        )

        parameterPairs.forEach { (proto, actual) ->
            if (actual.source == null) {
                // inferred types don't report INAPPLICABLE_CANDIDATE for type aliases!
                return@forEach
            }

            val upperBound = getSubstitutedUpperBound(proto, substitutor, context.session.typeContext)
            if (upperBound != null && !satisfiesBounds(upperBound, actual.coneType, context.session.typeContext)) {
                reporter.reportOn(actual.source, upperBound, context)
                return
            }

            // we must analyze nested things like
            // S<S<K, L>, T<K, L>>()
            actual.coneType.safeAs<ConeClassLikeType>()?.let {
                val errorOccurred = analyzeTypeParameters(it, context, reporter, context.session.typeContext, actual.source)

                if (errorOccurred) {
                    return
                }
            }
        }

        // if we're dealing with a constructor
        // resolved from a typealias we need to
        // check if our actual parameters satisfy
        // this constructor parameters.
        // e.g.
        // class B<T : Collection<Number>>
        // typealias A<G> = B<List<G>>
        // val a = A<Int>()
        when (calleeFir) {
            is FirConstructor -> analyzeConstructorCall(expression, substitutor, context.session.typeContext, reporter, context)
        }
    }

    private fun analyzeConstructorCall(
        functionCall: FirQualifiedAccessExpression,
        callSiteSubstitutor: ConeSubstitutor,
        typeSystemContext: ConeTypeContext,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        // holds Collection<Number> bound.
        // note that if B used another type parameter here,
        // we'd get Collection<K>. So we need to do one more
        // substitution here
        val protoConstructor = functionCall.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol.safeAs<FirConstructorSymbol>()
            ?.fir?.originalForSubstitutionOverride
            ?: return

        // holds Collection<G> bound.
        // we need to do substitution here to get
        // Collection<Int>
        val actualConstructor = functionCall.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol.safeAs<FirConstructorSymbol>()
            ?.fir.safeAs<FirConstructor>()
            ?.returnTypeRef?.coneType
            ?.safeAs<ConeClassLikeType>()
            ?: return

        val count = minOf(protoConstructor.typeParameters.size, actualConstructor.typeArguments.size)

        if (count == 0) {
            return
        }

        val constructorsParameterPairs = mutableMapOf<FirTypeParameterSymbol, ConeSimpleKotlinType>()

        for (it in 0 until count) {
            actualConstructor.typeArguments[it].safeAs<ConeSimpleKotlinType>()
                ?.let { that ->
                    if (that !is ConeClassErrorType) {
                        constructorsParameterPairs[protoConstructor.typeParameters[it].symbol] = that
                    } else {
                        return
                    }
                }
        }

        // we substitute typealias declaration
        // parameters to the ones used in the
        // typealias target
        val declarationSiteSubstitutor = substitutorByMap(
            constructorsParameterPairs.toMap().mapValues { it.value.type },
            context.session
        )

        constructorsParameterPairs.forEach { (proto, actual) ->
            // just in case
            var intersection = typeSystemContext.intersectTypes(
                proto.fir.bounds.map { it.coneType }
            ).safeAs<ConeKotlinType>() ?: return@forEach

            intersection = declarationSiteSubstitutor.substituteOrSelf(intersection)
            intersection = callSiteSubstitutor.substituteOrSelf(intersection)

            // substitute Int for G from
            // the example above
            val target = callSiteSubstitutor.substituteOrSelf(actual)
            val satisfiesBounds = AbstractTypeChecker.isSubtypeOf(typeSystemContext, target, intersection)

            if (!satisfiesBounds) {
                reporter.reportOn(functionCall.source, intersection, context)
                return
            }
        }
    }

    /**
     * Recursively analyzes type parameters
     * and reports the diagnostic on the given
     * reportTarget (because we can't report them
     * on type parameters themselves now).
     * Returns true if an error occured
     */
    private fun analyzeTypeParameters(
        type: ConeClassLikeType,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        typeSystemContext: ConeTypeContext,
        reportTarget: FirSourceElement?
    ): Boolean {
        val prototypeClass = type.lookupTag.toSymbol(context.session)
            ?.fir.safeAs<FirRegularClass>()
            ?: return false

        val count = minOf(prototypeClass.typeParameters.size, type.typeArguments.size)

        if (count == 0) {
            return false
        }

        val parameterPairs = mutableMapOf<FirTypeParameterSymbol, ConeClassLikeType>()

        for (it in 0 until count) {
            type.typeArguments[it].safeAs<ConeClassLikeType>()
                ?.let { that ->
                    if (that !is ConeClassErrorType) {
                        parameterPairs[prototypeClass.typeParameters[it].symbol] = that
                    } else {
                        return true
                    }
                }
        }

        val substitutor = substitutorByMap(
            parameterPairs.toMap().mapValues { it.value.type },
            context.session
        )

        parameterPairs.forEach { (proto, actual) ->
            val upperBound = getSubstitutedUpperBound(proto, substitutor, typeSystemContext)
            if (upperBound != null && !satisfiesBounds(upperBound, actual.type, typeSystemContext)) {
                // should report on the parameter instead!
                reporter.reportOn(reportTarget, upperBound, context)
                return true
            }

            val errorOccurred = analyzeTypeParameters(actual, context, reporter, typeSystemContext, reportTarget)

            if (errorOccurred) {
                return true
            }
        }

        return false
    }

    /**
     * Returns true if target satisfies the
     * bounds of the prototypeSymbol.
     */
    private fun satisfiesBounds(
        upperBound: ConeKotlinType,
        target: ConeKotlinType,
        typeSystemContext: ConeTypeContext
    ): Boolean {
        return AbstractTypeChecker.isSubtypeOf(typeSystemContext, target, upperBound, stubTypesEqualToAnything = false)
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

    private fun DiagnosticReporter.reportOn(
        source: FirSourceElement?,
        actual: ConeKotlinType,
        context: CheckerContext
    ) {
        reportOn(source, FirErrors.UPPER_BOUND_VIOLATED, actual, context)
    }
}
