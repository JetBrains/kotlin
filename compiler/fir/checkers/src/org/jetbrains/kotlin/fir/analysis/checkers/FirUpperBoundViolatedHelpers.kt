/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

/**
 * Recursively analyzes type parameters and reports the diagnostic on the given source calculated using typeRef
 */
fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isIgnoreTypeParameters: Boolean = false
) {
    val notExpandedType = typeRef?.coneTypeSafe<ConeClassLikeType>() ?: return

    // Everything should be reported on the typealias expansion
    if (notExpandedType.typeArguments.isEmpty()) return

    val type = notExpandedType.fullyExpandedType(context.session)
    val isAbbreviatedType = notExpandedType !== type

    val prototypeClassSymbol = type.lookupTag.toSymbol(context.session) as? FirRegularClassSymbol ?: return

    val typeParameterSymbols = prototypeClassSymbol.typeParameterSymbols

    if (typeParameterSymbols.isEmpty()) {
        return
    }

    val count = minOf(typeParameterSymbols.size, type.typeArguments.size)
    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()

    for (index in 0 until count) {
        val typeArgument = type.typeArguments[index]
        val typeParameterSymbol = typeParameterSymbols[index]

        val typeArgumentType = typeArgument.type
        if (typeArgumentType != null) {
            substitution[typeParameterSymbol] = typeArgumentType
        } else {
            substitution[typeParameterSymbol] =
                ConeStubTypeForTypeVariableInSubtyping(ConeTypeVariable("", typeParameterSymbol.toLookupTag()), ConeNullability.NOT_NULL)
        }
    }

    val substitutor = substitutorByMap(substitution, context.session)
    val typeArgumentsWithSourceInfo = type.typeArguments.withIndex().map { (index, projection) ->
        val (argTypeRef, source) =
            if (!isAbbreviatedType)
                extractArgumentTypeRefAndSource(typeRef, index) ?: return
            else
                // For abbreviated arguments we use the whole typeRef as a place to report
                FirTypeRefSource(null, typeRef.source)

        TypeArgumentWithSourceInfo(projection, argTypeRef, source)
    }

    return checkUpperBoundViolated(
        context, reporter, typeParameterSymbols, typeArgumentsWithSourceInfo, substitutor,
        isAbbreviatedType,
        isIgnoreTypeParameters,
    )
}

class TypeArgumentWithSourceInfo(
    val coneTypeProjection: ConeTypeProjection,
    val typeRef: FirTypeRef?,
    val source: KtSourceElement?,
)

fun checkUpperBoundViolated(
    context: CheckerContext,
    reporter: DiagnosticReporter,
    typeParameters: List<FirTypeParameterSymbol>,
    arguments: List<TypeArgumentWithSourceInfo>,
    substitutor: ConeSubstitutor,
    isAbbreviatedType: Boolean = false,
    isIgnoreTypeParameters: Boolean = false
) {
    val count = minOf(typeParameters.size, arguments.size)
    val typeSystemContext = context.session.typeContext

    for (index in 0 until count) {
        val argument = arguments.getOrNull(index) ?: continue
        val argumentType: ConeKotlinType? = argument.coneTypeProjection.type
        val argumentTypeRef = argument.typeRef
        val argumentSource = argument.source

        if (argumentType != null && argumentSource != null) {
            if (!isIgnoreTypeParameters || (argumentType.typeArguments.isEmpty() && argumentType !is ConeTypeParameterType)) {
                val intersection =
                    typeSystemContext.intersectTypes(typeParameters[index].resolvedBounds.map { it.coneType }) as? ConeKotlinType
                if (intersection != null) {
                    val upperBound = substitutor.substituteOrSelf(intersection)
                    if (!AbstractTypeChecker.isSubtypeOf(
                            typeSystemContext,
                            argumentType,
                            upperBound,
                            stubTypesEqualToAnything = true
                        )
                    ) {
                        val factory =
                            if (isAbbreviatedType) FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION else FirErrors.UPPER_BOUND_VIOLATED
                        reporter.reportOn(argumentSource, factory, upperBound, argumentType.type, context)
                        if (isAbbreviatedType) {
                            return
                        }
                    }
                }
            }

            checkUpperBoundViolated(argumentTypeRef, context, reporter, isIgnoreTypeParameters = isIgnoreTypeParameters)
        }
    }
}
