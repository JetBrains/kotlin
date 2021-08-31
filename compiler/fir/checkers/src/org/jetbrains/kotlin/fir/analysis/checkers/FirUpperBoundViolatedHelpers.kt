/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

/**
 * Recursively analyzes type parameters and reports the diagnostic on the given source calculated using typeRef
 * Returns true if an error occurred
 */
fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    typeParameters: List<FirTypeParameterSymbol>? = null,
    typeArguments: List<Any>? = null,
    typeArgumentRefsAndSources: List<FirTypeRefSource?>? = null,
    isTypeAlias: Boolean = false,
    isIgnoreTypeParameters: Boolean = false
) {
    val type = typeRef?.coneTypeSafe<ConeKotlinType>() ?: return

    val typeArgumentsCount = typeArguments?.size ?: type.typeArguments.size
    if (typeArgumentsCount == 0) {
        return
    }

    val typeParameterSymbols = typeParameters
        ?: if (type is ConeClassLikeType) {
            val fullyExpandedType = type.fullyExpandedType(context.session)
            val prototypeClassSymbol = fullyExpandedType.lookupTag.toSymbol(context.session) as? FirRegularClassSymbol ?: return
            if (type != fullyExpandedType) {
                // special check for type aliases
                checkUpperBoundViolated(
                    typeRef,
                    context,
                    reporter,
                    prototypeClassSymbol.typeParameterSymbols,
                    fullyExpandedType.typeArguments.toList(),
                    null,
                    isTypeAlias = true,
                    isIgnoreTypeParameters = isIgnoreTypeParameters
                )
                return
            }

            prototypeClassSymbol.typeParameterSymbols
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
        } else if (typeArgument is ConeTypeProjection) {
            val typeArgumentType = typeArgument.type
            if (typeArgumentType != null) {
                substitution[typeParameterSymbol] = typeArgumentType
            } else {
                substitution[typeParameterSymbol] =
                    ConeStubTypeForTypeVariableInSubtyping(ConeTypeVariable("", typeParameterSymbol.toLookupTag()), ConeNullability.NOT_NULL)
            }
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
            val localTypeArgument = type.typeArguments[index]
            if (localTypeArgument is ConeKotlinType) {
                typeArgument = localTypeArgument
            } else if (localTypeArgument is ConeKotlinTypeProjection) {
                typeArgument = localTypeArgument.type
            }
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
                val intersection =
                    typeSystemContext.intersectTypes(typeParameterSymbols[index].resolvedBounds.map { it.coneType }) as? ConeKotlinType
                if (intersection != null) {
                    val upperBound = substitutor.substituteOrSelf(intersection)
                    if (!AbstractTypeChecker.isSubtypeOf(
                            typeSystemContext,
                            typeArgument.type,
                            upperBound,
                            stubTypesEqualToAnything = true
                        )
                    ) {
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
