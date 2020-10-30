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
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.min
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

        val count = min(calleeFir.typeParameters.size, expression.typeArguments.size)

        if (count == 0) {
            return
        }

        val parameterPairs = mutableMapOf<FirTypeParameterSymbol, FirResolvedTypeRef>()

        for (it in 0 until count) {
            expression.typeArguments[it].safeAs<FirTypeProjectionWithVariance>()
                ?.typeRef.safeAs<FirResolvedTypeRef>()
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
            parameterPairs.mapValues { it.value.type }
        )

        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        parameterPairs.forEach { (proto, actual) ->
            if (actual.source == null) {
                // inferred types don't report INAPPLICABLE_CANDIDATE for type aliases!
                return@forEach
            }

            if (!satisfiesBounds(proto, actual.type, substitutor, typeCheckerContext)) {
                reporter.report(actual.source, proto, actual.type)
                return
            }

            // we must analyze nested things like
            // S<S<K, L>, T<K, L>>()
            actual.type.safeAs<ConeClassLikeType>()?.let {
                val errorOccurred = analyzeTypeParameters(it, context, reporter, typeCheckerContext, actual.source)

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
            is FirConstructor -> analyzeConstructorCall(expression, substitutor, typeCheckerContext, reporter)
        }
    }

    private fun analyzeConstructorCall(
        functionCall: FirQualifiedAccessExpression,
        callSiteSubstitutor: ConeSubstitutor,
        typeCheckerContext: AbstractTypeCheckerContext,
        reporter: DiagnosticReporter
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
            ?.returnTypeRef.safeAs<FirResolvedTypeRef>()
            ?.type.safeAs<ConeClassLikeType>()
            ?: return

        val count = min(protoConstructor.typeParameters.size, actualConstructor.typeArguments.size)

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
            constructorsParameterPairs.toMap().mapValues { it.value.type }
        )

        constructorsParameterPairs.forEach { (proto, actual) ->
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
                reporter.report(functionCall.source, proto, actual)
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
        typeCheckerContext: AbstractTypeCheckerContext,
        reportTarget: FirSourceElement?
    ): Boolean {
        val prototypeClass = type.lookupTag.toSymbol(context.session)
            ?.fir.safeAs<FirRegularClass>()
            ?: return false

        val count = min(prototypeClass.typeParameters.size, type.typeArguments.size)

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
            parameterPairs.toMap().mapValues { it.value.type }
        )

        parameterPairs.forEach { (proto, actual) ->
            if (!satisfiesBounds(proto, actual.type, substitutor, typeCheckerContext)) {
                // should report on the parameter instead!
                reporter.report(reportTarget, proto, actual)
                return true
            }

            val errorOccurred = analyzeTypeParameters(actual, context, reporter, typeCheckerContext, reportTarget)

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

    private fun DiagnosticReporter.report(source: FirSourceElement?, proto: FirTypeParameterSymbol, actual: ConeKotlinType) {
        source?.let {
            report(FirErrors.UPPER_BOUND_VIOLATED.on(it, proto, actual))
        }
    }
}
