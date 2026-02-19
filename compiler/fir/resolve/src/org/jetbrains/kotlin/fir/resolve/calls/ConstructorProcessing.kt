/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.toResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeCandidateCollector
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeCandidateCollector.TypeCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.scopeForClass
import org.jetbrains.kotlin.fir.scopes.scopeForTypeAlias
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.whileAnalysing
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

internal enum class ConstructorFilter {
    OnlyInner,
    OnlyNested,
    Both;

    fun accepts(memberDeclaration: FirMemberDeclaration): Boolean {
        return when (this) {
            Both -> true
            OnlyInner -> memberDeclaration.isInner
            OnlyNested -> !memberDeclaration.isInner
        }
    }
}

private fun FirScope.processConstructorsByName(
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    constructorFilter: ConstructorFilter,
    processor: (FirCallableSymbol<*>) -> Unit,
) {
    val (matchedClassifierSymbol, substitutor) = getFirstClassifierOrNull(callInfo, constructorFilter, bodyResolveComponents.session, bodyResolveComponents)
        ?: return
    val matchedClassSymbol = matchedClassifierSymbol as? FirClassLikeSymbol<*> ?: return

    context(bodyResolveComponents) {
        processConstructors(
            matchedClassSymbol,
            substitutor!!,
            processor,
        )
    }

    processSyntheticConstructors(
        matchedClassSymbol,
        processor,
        bodyResolveComponents
    )
}

internal fun FirScope.processFunctionsAndConstructorsByName(
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    constructorFilter: ConstructorFilter,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    processConstructorsByName(
        callInfo, bodyResolveComponents,
        constructorFilter,
        processor
    )

    processFunctionsByName(callInfo.name, processor)
}

private fun FirScope.getFirstClassifierOrNull(
    callInfo: CallInfo,
    constructorFilter: ConstructorFilter,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents
): TypeCandidate? {
    val collector = FirTypeCandidateCollector(session, bodyResolveComponents.file, bodyResolveComponents.containingDeclarations)

    fun process(symbol: FirClassifierSymbol<*>, substitutor: ConeSubstitutor) {
        val classifierDeclaration = symbol.fir
        if (classifierDeclaration is FirClassLikeDeclaration) {
            if (constructorFilter.accepts(classifierDeclaration)) {
                collector.processCandidate(symbol, substitutor, this@getFirstClassifierOrNull.toResolvedSymbolOrigin())
            }
        }
    }

    if (this is FirDefaultStarImportingScope) {
        processClassifiersByNameWithSubstitutionFromBothLevelsConditionally(callInfo.name) { symbol, substitutor ->
            process(symbol, substitutor)
            collector.applicability == CandidateApplicability.RESOLVED
        }
    } else {
        processClassifiersByNameWithSubstitution(callInfo.name, ::process)
    }

    return collector.getResult().resolvedCandidateOrNull()
}

private fun processSyntheticConstructors(
    matchedSymbol: FirClassLikeSymbol<*>,
    processor: (FirFunctionSymbol<*>) -> Unit,
    bodyResolveComponents: BodyResolveComponents
) {
    val samConstructor = bodyResolveComponents.samResolver.getSamConstructor(matchedSymbol.fir)
    if (samConstructor != null) {
        processor(samConstructor.symbol)
    }
}

context(c: SessionAndScopeSessionHolder)
private fun processConstructors(
    matchedSymbol: FirClassLikeSymbol<*>,
    substitutor: ConeSubstitutor,
    processor: (FirFunctionSymbol<*>) -> Unit,
) {
    whileAnalysing(c.session, matchedSymbol.fir) {
        val scope = when (matchedSymbol) {
            is FirTypeAliasSymbol -> {
                matchedSymbol.fir.scopeForTypeAlias(c.session, c.scopeSession)
            }
            is FirClassSymbol -> {
                val firClass = matchedSymbol.fir
                when (firClass.classKind) {
                    ClassKind.INTERFACE -> null
                    else -> firClass.scopeForClass(
                        substitutor,
                        firClass.symbol.toLookupTag(),
                        memberRequiredPhase = FirResolvePhase.STATUS,
                    )
                }
            }
        }

        scope?.processDeclaredConstructors {
            processor(it)
        }
    }
}
