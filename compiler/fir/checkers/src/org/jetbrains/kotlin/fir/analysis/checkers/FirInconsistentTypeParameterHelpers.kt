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
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

fun checkInconsistentTypeParameters(
    firTypeRefClasses: List<Pair<FirTypeRef?, FirRegularClassSymbol>>,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    source: KtSourceElement?,
    isValues: Boolean
) {
    val result = buildDeepSubstitutionMultimap(firTypeRefClasses, context)
    for ((typeParameterSymbol, typeAndProjections) in result) {
        val projections = typeAndProjections.projections
        if (projections.size > 1) {
            if (isValues) {
                reporter.reportOn(
                    source,
                    FirErrors.INCONSISTENT_TYPE_PARAMETER_VALUES,
                    typeParameterSymbol,
                    typeAndProjections.classSymbol,
                    projections,
                    context
                )
            } else {
                reporter.reportOn(
                    source,
                    FirErrors.INCONSISTENT_TYPE_PARAMETER_BOUNDS,
                    typeParameterSymbol,
                    typeAndProjections.classSymbol,
                    projections,
                    context
                )
            }
        }
    }
}

private fun buildDeepSubstitutionMultimap(
    firTypeRefClasses: List<Pair<FirTypeRef?, FirRegularClassSymbol>>,
    context: CheckerContext,
): Map<FirTypeParameterSymbol, ClassSymbolAndProjections> {
    val result = mutableMapOf<FirTypeParameterSymbol, ClassSymbolAndProjections>()
    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
    val visitedSupertypes = mutableSetOf<ConeKotlinType>()
    val session = context.session
    val typeContext = session.typeContext

    fun fillInDeepSubstitutor(typeArguments: Array<out ConeTypeProjection>?, classSymbol: FirRegularClassSymbol, context: CheckerContext) {
        if (typeArguments != null) {
            val typeParameterSymbols = classSymbol.typeParameterSymbols
            val count = minOf(typeArguments.size, typeParameterSymbols.size)

            for (index in 0 until count) {
                val typeArgument = typeArguments[index]

                val substitutedArgument = ConeSubstitutorByMap(substitution, session).substituteArgument(
                    typeArgument,
                    classSymbol.toLookupTag(),
                    index
                ) ?: typeArgument
                val substitutedType = substitutedArgument.type ?: continue

                val typeParameterSymbol = typeParameterSymbols[index]

                substitution[typeParameterSymbol] = substitutedType
                var classSymbolAndProjections = result[typeParameterSymbol]
                val projections: MutableList<ConeKotlinType>
                if (classSymbolAndProjections == null) {
                    projections = mutableListOf()
                    classSymbolAndProjections = ClassSymbolAndProjections(classSymbol, projections)
                    result[typeParameterSymbol] = classSymbolAndProjections
                } else {
                    projections = classSymbolAndProjections.projections
                }

                if (projections.all {
                        it != substitutedType && !AbstractTypeChecker.equalTypes(typeContext, it, substitutedType)
                    }) {
                    projections.add(substitutedType)
                }
            }
        }

        for (superTypeRef in classSymbol.resolvedSuperTypeRefs) {
            val fullyExpandedType = superTypeRef.coneType.fullyExpandedType(session)
            if (!visitedSupertypes.add(fullyExpandedType))
                return

            val superClassSymbol = fullyExpandedType.toRegularClassSymbol(session)
            withSuppressedDiagnostics(superTypeRef, context) {
                if (!fullyExpandedType.isEnum && superClassSymbol != null) {
                    fillInDeepSubstitutor(fullyExpandedType.typeArguments, superClassSymbol, it)
                }
            }
        }
    }

    for (firTypeRefClass in firTypeRefClasses) {
        fillInDeepSubstitutor(firTypeRefClass.first?.coneType?.fullyExpandedType(session)?.typeArguments, firTypeRefClass.second, context)
    }
    return result
}

private data class ClassSymbolAndProjections(
    val classSymbol: FirRegularClassSymbol,
    val projections: MutableList<ConeKotlinType>
)
