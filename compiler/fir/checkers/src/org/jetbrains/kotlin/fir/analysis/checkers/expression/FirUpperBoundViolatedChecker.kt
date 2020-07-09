/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirUpperBoundViolatedChecker : FirQualifiedAccessChecker() {
    override fun check(functionCall: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleeFir = functionCall.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol
            ?.fir.safeAs<FirTypeParameterRefsOwner>()
            ?: return

        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        val parameterPairs = calleeFir.typeParameters.zip(functionCall.typeArguments)
            .map { (proto, actual) ->
                proto.symbol to actual.safeAs<FirTypeProjectionWithVariance>()
                    ?.typeRef.safeAs<FirResolvedTypeRef>()
            }
            .toMapWithoutNulls()

        // we substitute actual values to the
        // type parameters from the declaration
        val substitutor = substitutorByMap(
            parameterPairs.mapValues { it.value.type }
        )

        parameterPairs.forEach { proto, actual ->
            if (!satisfiesBounds(proto, actual.type, substitutor, typeCheckerContext)) {
                reporter.report(actual.source)
                return@forEach
            }

            // we must analyze nested things like
            // S<S<K, L>, T<K, L>>()
            actual.type.safeAs<ConeClassLikeType>()?.let {
                analyzeTypeParameters(it, context, reporter, typeCheckerContext, actual.source)
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
            is FirConstructor -> analyzeConstructorCall(functionCall, substitutor, typeCheckerContext, reporter)
        }
    }

    private fun analyzeConstructorCall(
        functionCall: FirQualifiedAccessExpression,
        callSiteSubstitutor: ConeSubstitutor,
        typeCheckerContext: AbstractTypeCheckerContext,
        reporter: DiagnosticReporter,
    ) {
        // holds Collection<Number> bound.
        // note that if B used another type parameter here,
        // we'd get Collection<K>. So we need to do one more
        // substitution here
        val protoConstructor = functionCall.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol.safeAs<FirConstructorSymbol>()
            ?.overriddenSymbol
            ?.fir.safeAs<FirConstructor>()
            ?: return

        // holds Collection<G> bound.
        // we need to do substitution here to get
        // Collection<Int>
        val actualConstructor = functionCall.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol.safeAs<FirConstructorSymbol>()
            ?.fir.safeAs<FirConstructor>()
            ?.returnTypeRef.safeAs<FirResolvedTypeRef>()
            ?.type.safeAs<ConeClassLikeType>()
            ?: return

        val constructorsParameterPairs = protoConstructor.typeParameters
            .zip(actualConstructor.typeArguments)
            .map { (proto, actual) ->
                proto.symbol to actual.safeAs<ConeSimpleKotlinType>()
            }
            .toMapWithoutNulls()

        // we substitute typealias declaration
        // parameters to the ones used in the
        // typealias target
        val declarationSiteSubstitutor = substitutorByMap(
            constructorsParameterPairs.mapValues { it.value.type }
        )

        constructorsParameterPairs.forEach { proto, actual ->
            // just in case
            var intersection = typeCheckerContext.intersectTypes(
                proto.fir.bounds.filterIsInstance<FirResolvedTypeRef>().map { it.type }
            ).safeAs<ConeKotlinType>() ?: return@forEach

            intersection = declarationSiteSubstitutor.substituteOrSelf(intersection)
            intersection = callSiteSubstitutor.substituteOrSelf(intersection)

            // substitute Int for G from
            // the example above
            val target = callSiteSubstitutor.substituteOrSelf(actual)
            val satisfiesBounds = AbstractTypeChecker.isSubtypeOf(typeCheckerContext, target, intersection)

            if (!satisfiesBounds) {
                reporter.report(functionCall.source)
                return@forEach
            }
        }
    }

    /**
     * Recursively analyzes type parameters
     * and reports the diagnostic on the given
     * reportTarget (because we can't report them
     * on type parameters themselves now).
     */
    private fun analyzeTypeParameters(
        type: ConeClassLikeType,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        typeCheckerContext: AbstractTypeCheckerContext,
        reportTarget: FirSourceElement?
    ) {
        val prototypeClass = type.lookupTag.toSymbol(context.session)
            ?.fir.safeAs<FirRegularClass>()
            ?: return

        val parameterPairs = prototypeClass.typeParameters.zip(type.typeArguments)
            .map { (proto, actual) ->
                proto.symbol to actual.safeAs<ConeClassLikeType>()
            }
            .toMapWithoutNulls()

        val substitutor = substitutorByMap(
            parameterPairs.mapValues { it.value.type }
        )

        parameterPairs.forEach { proto, actual ->
            if (!satisfiesBounds(proto, actual.type, substitutor, typeCheckerContext)) {
                // should report on the parameter instead!
                reporter.report(reportTarget)
                return@forEach
            }

            analyzeTypeParameters(actual, context, reporter, typeCheckerContext, reportTarget)
        }
    }

    /**
     * Returns true if target satisfies the
     * bounds of the prototypeSymbol.
     */
    private fun satisfiesBounds(
        prototypeSymbol: FirTypeParameterSymbol,
        target: ConeKotlinType,
        substitutor: ConeSubstitutor,
        typeCheckerContext: AbstractTypeCheckerContext
    ): Boolean {
        var intersection = typeCheckerContext.intersectTypes(
            prototypeSymbol.fir.bounds.filterIsInstance<FirResolvedTypeRef>().map { it.type }
        ).safeAs<ConeKotlinType>() ?: return true

        intersection = substitutor.substituteOrSelf(intersection)
        return AbstractTypeChecker.isSubtypeOf(typeCheckerContext, target, intersection)
    }

    /**
     * Removes the entries where either A? or B?
     * is null and constructs a Map<A, B>.
     */
    private fun <A, B> List<Pair<A?, B?>>.toMapWithoutNulls() = this
        .filter { it.first != null && it.second != null }
        .map { it.first!! to it.second!! }
        .toMap()

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.UPPER_BOUND_VIOLATED.on(it))
        }
    }
}