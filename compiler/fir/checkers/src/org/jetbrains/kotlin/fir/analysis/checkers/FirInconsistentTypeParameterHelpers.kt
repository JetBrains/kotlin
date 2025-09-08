/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkInconsistentTypeParameters(
    firTypeRefClasses: List<Pair<FirTypeRef?, FirClassSymbol<*>>>,
    source: KtSourceElement?,
    isValues: Boolean,
) {
    val result = buildDeepSubstitutionMultimap(firTypeRefClasses)
    for ((typeParameterSymbol, typeAndProjections) in result) {
        val projections = typeAndProjections.projections
        if (projections.size > 1) {
            val diagnosticFactory =
                if (isValues) FirErrors.INCONSISTENT_TYPE_PARAMETER_VALUES else FirErrors.INCONSISTENT_TYPE_PARAMETER_BOUNDS
            reporter.reportOn(
                source,
                diagnosticFactory,
                typeParameterSymbol,
                typeAndProjections.classSymbol,
                // Report `Any?` instead of `*` for star projections because diagnostics renderer doesn't support type projections
                // Moreover, K1 report `Any?` instead of `*`
                projections.map { it.type ?: StandardTypes.NullableAny })
        }
    }
}

context(context: CheckerContext)
private fun buildDeepSubstitutionMultimap(
    firTypeRefClasses: List<Pair<FirTypeRef?, FirClassSymbol<*>>>,
): Map<FirTypeParameterSymbol, ClassSymbolAndProjections> {
    val result = mutableMapOf<FirTypeParameterSymbol, ClassSymbolAndProjections>()
    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
    val session = context.session
    val typeContext = session.typeContext
    val substitutor = FE10LikeConeSubstitutor(substitution, session)
    val visitedSupertypes = mutableSetOf<ConeKotlinType>()

    context(context: CheckerContext)
    fun fillInDeepSubstitutor(
        typeArguments: Array<out ConeTypeProjection>?,
        classSymbol: FirClassSymbol<*>
    ) {
        if (typeArguments != null) {
            val typeParameterSymbols = classSymbol.typeParameterSymbols
            val count = minOf(typeArguments.size, typeParameterSymbols.size)

            for (index in 0 until count) {
                val typeArgument = typeArguments[index]

                val substitutedArgument = substitutor.substituteArgument(typeArgument, index) ?: typeArgument

                val typeParameterSymbol = typeParameterSymbols[index]

                substitution[typeParameterSymbol] = substitutedArgument
                val projections = result.getOrPut(typeParameterSymbol) {
                    ClassSymbolAndProjections(classSymbol, mutableListOf())
                }.projections

                val substitutedArgumentType = substitutedArgument.type
                if (projections.none {
                        when {
                            // One of them is a star projection
                            substitutedArgumentType == null || it.type == null -> it === substitutedArgument
                            // None of them is a star projection
                            else -> AbstractTypeChecker.equalTypes(typeContext, it.type!!, substitutedArgumentType)
                        }
                    }
                ) {
                    projections.add(substitutedArgument)
                }
            }
        }

        for (superTypeRef in classSymbol.resolvedSuperTypeRefs) {
            val fullyExpandedType = superTypeRef.coneType.fullyExpandedType()
            if (!visitedSupertypes.add(fullyExpandedType)) continue

            val superClassSymbol = fullyExpandedType.toRegularClassSymbol()
            if (!fullyExpandedType.isEnum && superClassSymbol != null) {
                fillInDeepSubstitutor(fullyExpandedType.typeArguments, superClassSymbol)
            }
        }
    }

    for ((typeRef, regularClassSymbol) in firTypeRefClasses) {
        fillInDeepSubstitutor(typeRef?.coneType?.fullyExpandedType()?.typeArguments, regularClassSymbol)
    }
    return result
}

private data class ClassSymbolAndProjections(
    val classSymbol: FirClassSymbol<*>,
    val projections: MutableList<ConeTypeProjection>
)
