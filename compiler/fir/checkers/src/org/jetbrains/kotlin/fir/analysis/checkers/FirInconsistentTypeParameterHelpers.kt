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
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

fun checkInconsistentTypeParameters(
    firTypeRefClasses: List<Pair<FirTypeRef?, FirRegularClass>>,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    source: FirSourceElement?,
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
    firTypeRefClasses: List<Pair<FirTypeRef?, FirRegularClass>>,
    context: CheckerContext,
): Map<FirTypeParameterSymbol, ClassSymbolAndProjections> {
    val result = mutableMapOf<FirTypeParameterSymbol, ClassSymbolAndProjections>()
    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
    val session = context.session
    val typeContext = session.typeContext

    fun fillInDeepSubstitutor(typeArguments: Array<out ConeTypeProjection>?, firClass: FirRegularClass) {
        if (typeArguments != null) {
            val typeParameters = firClass.typeParameters
            val count = minOf(typeArguments.size, typeParameters.size)

            for (index in 0 until count) {
                val typeArgument = typeArguments[index]

                val substitutedArgument = ConeSubstitutorByMap(substitution, session).substituteArgument(typeArgument) ?: typeArgument
                val substitutedType = substitutedArgument.type ?: continue

                val typeParameterSymbol = typeParameters[index].symbol

                substitution[typeParameterSymbol] = substitutedType
                var classSymbolAndProjections = result[typeParameterSymbol]
                val projections: MutableList<ConeKotlinType>
                if (classSymbolAndProjections == null) {
                    projections = mutableListOf()
                    classSymbolAndProjections = ClassSymbolAndProjections(firClass.symbol, projections)
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

        for (superTypeRef in firClass.superTypeRefs) {
            withSuppressedDiagnostics(superTypeRef, context) {
                val fullyExpandedType = superTypeRef.coneType.fullyExpandedType(session)
                val superTypeClass = fullyExpandedType.toRegularClass(session)
                if (!fullyExpandedType.isEnum && superTypeClass != null) {
                    fillInDeepSubstitutor(fullyExpandedType.typeArguments, superTypeClass)
                }
            }
        }
    }

    for (firTypeRefClass in firTypeRefClasses) {
        fillInDeepSubstitutor(firTypeRefClass.first?.coneType?.fullyExpandedType(session)?.typeArguments, firTypeRefClass.second)
    }
    return result
}

private data class ClassSymbolAndProjections(
    val classSymbol: FirRegularClassSymbol,
    val projections: MutableList<ConeKotlinType>
)