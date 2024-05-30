/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.FirTypeRefSource
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentsTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.createParametersSubstitutor
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.mapParametersToArgumentsOf
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance

object FirProjectionRelationChecker : FirTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef.source?.kind?.shouldSkipErrorTypeReporting != false) return
        val type = typeRef.coneTypeSafe<ConeClassLikeType>()
        val fullyExpandedType = type?.fullyExpandedType(context.session) ?: return

        val potentiallyProblematicArguments = collectPotentiallyProblematicArguments(typeRef, context.session)

        for (argumentData in potentiallyProblematicArguments) {
            val declaration = argumentData.constructor.toRegularClassSymbol(context.session)
                ?: error("Shouldn't be here")
            val proto = declaration.typeParameterSymbols[argumentData.index]
            val actual = argumentData.projection
            val protoVariance = proto.variance

            val projectionRelation = if (actual is ConeKotlinTypeConflictingProjection ||
                actual is ConeKotlinTypeProjectionIn && protoVariance == Variance.OUT_VARIANCE ||
                actual is ConeKotlinTypeProjectionOut && protoVariance == Variance.IN_VARIANCE
            ) {
                ProjectionRelation.Conflicting
            } else if (actual is ConeKotlinTypeProjectionIn && protoVariance == Variance.IN_VARIANCE ||
                actual is ConeKotlinTypeProjectionOut && protoVariance == Variance.OUT_VARIANCE
            ) {
                ProjectionRelation.Redundant
            } else {
                ProjectionRelation.None
            }

            val argTypeRefSource = argumentData.source

            if (projectionRelation != ProjectionRelation.None && typeRef.source?.kind !is KtFakeSourceElementKind) {
                reporter.reportOn(
                    argTypeRefSource.source ?: argTypeRefSource.typeRef?.source,
                    if (projectionRelation == ProjectionRelation.Conflicting)
                        if (type != fullyExpandedType) FirErrors.CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION else FirErrors.CONFLICTING_PROJECTION
                    else
                        FirErrors.REDUNDANT_PROJECTION,
                    fullyExpandedType,
                    context
                )
            }
        }
    }

    private data class TypeArgumentData(
        val constructor: ConeKotlinType,
        val index: Int,
        val projection: ConeTypeProjection,
        val source: FirTypeRefSource,
    )

    private fun extractImmediateTypeArgumentData(typeRef: FirTypeRef): List<TypeArgumentData> =
        extractArgumentsTypeRefAndSource(typeRef)
            ?.let { typeRef.coneType.typeArguments.zip(it) }
            ?.mapIndexed { index, it ->
                TypeArgumentData(typeRef.coneType, index, it.first, it.second)
            }
            .orEmpty()

    /**
     * When checking projections, we have two kinds of independent traversals:
     *
     * 1. Traversing arguments of arguments like `in Number` in `GenericA<GenericB<in Number>>`
     * 2. Expanding typealiases like `TA<in Number>` for `TA<T> == GenericC<String, GenericB<T>>`
     *
     * This function starts (2) for the non-expanded type, which then also does (1) at every step of (2).
     * Note that it does not do (1) for the non-expanded type: this work is done by the checker
     * runner, so [org.jetbrains.kotlin.fir.analysis.checkers.type.FirProjectionRelationChecker]
     * will be called for all nested typerefs within the non-expanded typeref.
     *
     * The logic for (1) is separated into two places for the following reason: we have different
     * conditions that signify if an argument is potentially problematic.
     * For the original non-expanded type, an argument is suspicious if [canBeProblematic].
     * For (1) traversals of types obtained through expansions, an argument is suspicious if it's
     * a type parameter.
     * We don't need to consider [canBeProblematic] for this case, as typealias expansions are
     * already checked on typealias declaration sites, so have already reported an error there.
     *
     * Throughout (2) we are going to maintain a `parametersToSources` map: it maps parameters
     * of typealiases to some source elements where the use site non-expanded type substitutes
     * a specific a value to what ultimately flows to this type parameter during expansions.
     * But this map doesn't contain mappings for all possible parameters or use site argument
     * sources.
     * Instead, it'll only refer to argument that [canBeProblematic].
     * Again, if sudden conflicting `in` or `out` arise during the expansions, those will have been
     * reported at typealias declaration sites, so we can skip them.
     */
    private fun collectPotentiallyProblematicArguments(typeRef: FirTypeRef, session: FirSession): List<TypeArgumentData> {
        val shallowArgumentsData = extractImmediateTypeArgumentData(typeRef).filter { it.projection.kind.canBeProblematic }
        val symbol = typeRef.coneType.toSymbol(session)

        if (symbol !is FirTypeAliasSymbol) {
            return shallowArgumentsData
        }

        val projectionToSource = shallowArgumentsData.associateBy(
            keySelector = { it.projection },
            valueTransform = { it.source },
        )

        return buildList {
            collectPotentiallyProblematicArgumentsFromTypeAliasExpansion(
                symbol, typeRef.coneType, ConeSubstitutor.Empty, this, session,
                argumentIndexToSource = { projectionToSource[typeRef.coneType.typeArguments[it]] },
            )
        }
    }

    private fun collectPotentiallyProblematicArguments(
        type: ConeKotlinType,
        previousSubstitutor: ConeSubstitutor,
        parametersToSources: Map<FirTypeParameterSymbol, FirTypeRefSource>,
        result: MutableList<TypeArgumentData>,
        session: FirSession,
    ) {
        if (type !is ConeClassLikeType) {
            return
        }

        val symbol = type.toSymbol(session)

        if (symbol is FirTypeAliasSymbol) {
            return collectPotentiallyProblematicArgumentsFromTypeAliasExpansion(
                symbol, type, previousSubstitutor, result, session,
                argumentIndexToSource = { parametersToSources[type.typeArguments[it].type?.toSymbol(session)] },
            )
        }

        val substitutedType = previousSubstitutor.substituteOrSelf(type)

        for ((index, argument) in type.typeArguments.withIndex()) {
            val unsubstitutedType = argument.type ?: continue
            collectPotentiallyProblematicArguments(unsubstitutedType, previousSubstitutor, parametersToSources, result, session)

            if (unsubstitutedType !is ConeTypeParameterType) {
                continue
            }

            val substitutedArgument = previousSubstitutor.substituteArgument(argument, index) ?: continue
            val source = (argument.type as? ConeTypeParameterType)?.toSymbol(session)?.let { parametersToSources[it] }
                ?: error("Should have calculated")
            result += TypeArgumentData(substitutedType, index, substitutedArgument, source)
        }
    }

    private inline fun collectPotentiallyProblematicArgumentsFromTypeAliasExpansion(
        symbol: FirTypeAliasSymbol,
        type: ConeKotlinType,
        previousSubstitutor: ConeSubstitutor,
        result: MutableList<TypeArgumentData>,
        session: FirSession,
        argumentIndexToSource: (Int) -> FirTypeRefSource?,
    ) {
        symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        val alias = @OptIn(SymbolInternals::class) symbol.fir
        val typeAliasMap = alias.mapParametersToArgumentsOf(type)
        val indices = typeAliasMap.indices
        val sources = indices.map(argumentIndexToSource)
        val interestingIndices = indices.filter { sources[it] != null }.takeIf { it.isNotEmpty() } ?: return

        val typeAliasMapOfPotentiallyProblematicParameters = interestingIndices.associateBy(
            keySelector = { typeAliasMap[it].first },
            valueTransform = { typeAliasMap[it].second },
        )
        val nextStepSubstitutor = createParametersSubstitutor(session, typeAliasMapOfPotentiallyProblematicParameters)
        val parametersToSources = interestingIndices.associateBy(
            keySelector = { typeAliasMap[it].first },
            valueTransform = { sources[it] ?: error("Should have calculated") },
        )

        collectPotentiallyProblematicArguments(
            alias.expandedTypeRef.coneType,
            nextStepSubstitutor.chain(previousSubstitutor),
            parametersToSources, result, session,
        )
    }

    private val ProjectionKind.canBeProblematic get() = this == ProjectionKind.IN || this == ProjectionKind.OUT

    private enum class ProjectionRelation {
        Conflicting,
        Redundant,
        None
    }
}
