/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.FirTypeRefSource
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirUpperBoundViolatedClassChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirClass<*>) {
            for (typeParameter in declaration.typeParameters) {
                if (typeParameter is FirTypeParameter) {
                    for (bound in typeParameter.bounds) {
                        checkUpperBoundViolated(bound, context, reporter)
                    }
                }
            }

            for (superTypeRef in declaration.superTypeRefs) {
                checkUpperBoundViolated(superTypeRef, context, reporter)
            }
        } else if (declaration is FirTypeAlias) {
            checkUpperBoundViolated(declaration.expandedTypeRef, context, reporter, isIgnoreTypeParameters = true)
        } else if (declaration is FirCallableDeclaration<*>) {
            if (declaration.returnTypeRef.source?.kind !is FirFakeSourceElementKind) {
                checkUpperBoundViolated(declaration.returnTypeRef, context, reporter)
            }
        }
    }
}

object FirUpperBoundViolatedExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleReference = expression.calleeReference
        var calleeFir: FirTypeParameterRefsOwner? = null
        if (calleReference is FirResolvedNamedReference) {
            calleeFir = calleReference.safeAs<FirResolvedNamedReference>()?.resolvedSymbol?.fir.safeAs()
        } else if (calleReference is FirErrorNamedReference) {
            val diagnostic = calleReference.diagnostic
            if (diagnostic is ConeInapplicableCandidateError &&
                diagnostic.applicability == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
            ) {
                return
            }
            calleeFir = calleReference.candidateSymbol?.fir.safeAs()
        }

        var typeArguments: List<Any>? = null
        var typeArgumentRefsAndSources: List<FirTypeRefSource?>? = null
        val typeParameters = if (calleeFir is FirConstructor) {
            typeArgumentRefsAndSources =
                expression.typeArguments.map { FirTypeRefSource((it as? FirTypeProjectionWithVariance)?.typeRef, it.source) }
            val prototypeClass = calleeFir.dispatchReceiverType?.toSymbol(context.session)?.fir.safeAs<FirRegularClass>()
            prototypeClass?.typeParameters?.map { it.symbol }
        } else {
            typeArguments = expression.typeArguments
            calleeFir?.typeParameters?.map { it.symbol }
        }

        checkUpperBoundViolated(
            expression.typeRef,
            context,
            reporter,
            typeParameters,
            typeArguments,
            typeArgumentRefsAndSources
        )
    }
}

/**
 * Recursively analyzes type parameters and reports the diagnostic on the given source calculated using typeRef
 * Returns true if an error occurred
 */
private fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    typeParameters: List<FirTypeParameterSymbol>? = null,
    typeArguments: List<Any>? = null,
    typeArgumentRefsAndSources: List<FirTypeRefSource?>? = null,
    isTypeAlias: Boolean = false,
    isIgnoreTypeParameters: Boolean = false
) {
    val type = when (typeRef) {
        is ConeKotlinType -> typeRef
        is FirResolvedTypeRef -> typeRef.type
        else -> return
    }

    val typeArgumentsCount = typeArguments?.size ?: type.typeArguments.size
    if (typeArgumentsCount == 0) {
        return
    }

    val typeParameterSymbols = typeParameters
        ?: if (type is ConeClassLikeType) {
            val fullyExpandedType = type.fullyExpandedType(context.session)
            val prototypeClass = fullyExpandedType.lookupTag.toSymbol(context.session)
                ?.fir.safeAs<FirRegularClass>()
                ?: return

            if (type != fullyExpandedType) {
                // special check for type aliases
                checkUpperBoundViolated(
                    typeRef,
                    context,
                    reporter,
                    prototypeClass.typeParameters.map { it.symbol },
                    fullyExpandedType.typeArguments.toList(),
                    null,
                    isTypeAlias = true,
                    isIgnoreTypeParameters = isIgnoreTypeParameters
                )
                return
            }

            prototypeClass.typeParameters.map { it.symbol }
        } else {
            listOf()
        }

    if (typeParameterSymbols.isEmpty()) {
        return
    }

    val count = minOf(typeParameterSymbols.size, typeArgumentsCount)
    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()

    for (index in 0 until count) {
        val typeArgument = typeArguments?.elementAt(index) ?: type.typeArguments[index]
        val typeParameterSymbol = typeParameterSymbols[index]

        if (typeArgument is FirTypeProjectionWithVariance) {
            substitution[typeParameterSymbol] = typeArgument.typeRef.coneType
        } else if (typeArgument is ConeKotlinType) {
            substitution[typeParameterSymbol] = typeArgument.type
        }
    }

    val substitutor = substitutorByMap(substitution, context.session)
    val typeSystemContext = context.session.typeContext

    for (index in 0 until count) {
        var typeArgument: ConeKotlinType? = null
        var typeArgumentTypeRef: FirTypeRef? = null
        var typeArgumentSource: FirSourceElement? = null

        if (typeArguments != null) {
            val localTypeArgument = typeArguments[index]
            if (localTypeArgument is FirTypeProjectionWithVariance) {
                typeArgumentTypeRef = localTypeArgument.typeRef
                typeArgument = typeArgumentTypeRef.coneType
                typeArgumentSource = localTypeArgument.source
            } else if (localTypeArgument is ConeKotlinType) {
                // Typealias case
                typeArgument = localTypeArgument
                typeArgumentSource = typeRef.source
            }
        } else {
            typeArgument = type.typeArguments[index] as? ConeKotlinType
            val typeArgumentRefAndSource = typeArgumentRefsAndSources?.elementAtOrNull(index)
            if (typeArgumentRefAndSource != null) {
                typeArgumentTypeRef = typeArgumentRefAndSource.typeRef
                typeArgumentSource = typeArgumentRefAndSource.source
            } else {
                val extractedTypeArgumentRefAndSource = extractArgumentTypeRefAndSource(typeRef, index)
                if (extractedTypeArgumentRefAndSource != null) {
                    typeArgumentTypeRef = extractedTypeArgumentRefAndSource.typeRef
                    typeArgumentSource = extractedTypeArgumentRefAndSource.source
                }
            }
        }

        if (typeArgument != null && typeArgumentSource != null) {
            if (!isIgnoreTypeParameters || (typeArgument.typeArguments.isEmpty() && typeArgument !is ConeTypeParameterType)) {
                val intersection = typeSystemContext.intersectTypes(typeParameterSymbols[index].fir.bounds.map { it.coneType }) as? ConeKotlinType
                if (intersection != null) {
                    val upperBound = substitutor.substituteOrSelf(intersection)
                    if (!AbstractTypeChecker.isSubtypeOf(typeSystemContext, typeArgument.type, upperBound, stubTypesEqualToAnything = false)) {
                        val factory =
                            if (isTypeAlias) FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION else FirErrors.UPPER_BOUND_VIOLATED
                        reporter.reportOn(typeArgumentSource, factory, upperBound, typeArgument.type, context)
                        if (isTypeAlias) {
                            return
                        }
                    }
                }
            }

            checkUpperBoundViolated(typeArgumentTypeRef, context, reporter, isIgnoreTypeParameters = isIgnoreTypeParameters)
        }
    }
}