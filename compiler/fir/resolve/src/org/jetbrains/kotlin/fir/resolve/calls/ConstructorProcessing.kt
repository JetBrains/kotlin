/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.TypeAliasConstructorsSubstitutingScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.fir.whileAnalysing
import org.jetbrains.kotlin.name.Name

private operator fun <T> Pair<T, *>?.component1(): T? = this?.first
private operator fun <T> Pair<*, T>?.component2(): T? = this?.second

internal enum class ConstructorFilter(val acceptInner: Boolean, val acceptNested: Boolean) {
    OnlyInner(acceptInner = true, acceptNested = false),
    OnlyNested(acceptInner = false, acceptNested = true),
    Both(acceptInner = true, acceptNested = true),
}

private fun FirScope.processConstructorsByName(
    callInfo: CallInfo,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    constructorFilter: ConstructorFilter,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    val (matchedClassifierSymbol, substitutor) = getFirstClassifierOrNull(callInfo, constructorFilter, session, bodyResolveComponents) ?: return
    val matchedClassSymbol = matchedClassifierSymbol as? FirClassLikeSymbol<*> ?: return

    processConstructors(
        matchedClassSymbol,
        substitutor,
        processor,
        session,
        bodyResolveComponents,
        constructorFilter
    )

    processSyntheticConstructors(
        matchedClassSymbol,
        processor,
        bodyResolveComponents
    )
}

internal fun FirScope.processFunctionsAndConstructorsByName(
    callInfo: CallInfo,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    constructorFilter: ConstructorFilter,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    processConstructorsByName(
        callInfo, session, bodyResolveComponents,
        constructorFilter,
        processor
    )

    processFunctionsByName(callInfo.name, processor)
}

private data class SymbolWithSubstitutor(val symbol: FirClassifierSymbol<*>, val substitutor: ConeSubstitutor)

fun FirScope.getSingleVisibleClassifier(
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    name: Name
): FirClassifierSymbol<*>? {
    var result: FirClassifierSymbol<*>? = null
    var isAmbiguousResult = false
    processClassifiersByName(name) { classifierSymbol ->
        if (!classifierSymbol.fir.isInvisibleOrHidden(session, bodyResolveComponents)) {
            if (result == classifierSymbol) return@processClassifiersByName

            if (result == null) {
                result = classifierSymbol
            } else {
                val checkResult = checkUnambiguousClassifiers(result!!, classifierSymbol, session)
                if (checkResult.shouldReplaceResult) {
                    result = classifierSymbol
                } else {
                    isAmbiguousResult = checkResult.isAmbiguousResult
                }
            }
        }
    }
    return result.takeUnless { isAmbiguousResult }
}

private fun FirDeclaration.isInvisibleOrHidden(session: FirSession, bodyResolveComponents: BodyResolveComponents): Boolean {
    if (this is FirMemberDeclaration) {
        if (!session.visibilityChecker.isVisible(
                this,
                session,
                bodyResolveComponents.file,
                bodyResolveComponents.containingDeclarations,
                dispatchReceiver = null,
                isCallToPropertySetter = false
            )
        ) {
            return true
        }
    }

    return symbol.isDeprecationLevelHidden(session)
}

private fun FirScope.getFirstClassifierOrNull(
    callInfo: CallInfo,
    constructorFilter: ConstructorFilter,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents
): SymbolWithSubstitutor? {
    var isSuccessResult = false
    var isAmbiguousResult = false
    var result: SymbolWithSubstitutor? = null

    fun process(symbol: FirClassifierSymbol<*>, substitutor: ConeSubstitutor) {
        val classifierDeclaration = symbol.fir
        var isSuccessCandidate = !classifierDeclaration.isInvisibleOrHidden(session, bodyResolveComponents)
        if (classifierDeclaration is FirClassLikeDeclaration) {
            val acceptedByFilter = when (classifierDeclaration.isInner) {
                true -> constructorFilter.acceptInner
                false -> constructorFilter.acceptNested
            }
            isSuccessCandidate = isSuccessCandidate && acceptedByFilter
        }

        when {
            isSuccessCandidate && !isSuccessResult -> {
                // successful result is better than unsuccessful
                isSuccessResult = true
                isAmbiguousResult = false
                result = SymbolWithSubstitutor(symbol, substitutor)
            }
            result?.symbol === symbol -> {
                // skip identical results
                return
            }
            result != null -> {
                if (isSuccessResult == isSuccessCandidate) {
                    val checkResult = checkUnambiguousClassifiers(result!!.symbol, symbol, session)
                    if (checkResult.shouldReplaceResult) {
                        result = SymbolWithSubstitutor(symbol, substitutor)
                    } else {
                        isAmbiguousResult = checkResult.isAmbiguousResult
                    }
                } else {
                    // ignore unsuccessful result if we have successful one
                }
            }
            else -> {
                // result == null: any result is better than no result
                isSuccessResult = isSuccessCandidate
                result = SymbolWithSubstitutor(symbol, substitutor)
            }
        }
    }

    if (this is FirDefaultStarImportingScope) {
        processClassifiersByNameWithSubstitutionFromBothLevelsConditionally(callInfo.name) { symbol, substitutor ->
            process(symbol, substitutor)
            isSuccessResult
        }
    } else {
        processClassifiersByNameWithSubstitution(callInfo.name, ::process)
    }

    return result.takeUnless { isAmbiguousResult }
}

private data class CheckUnambiguousClassifiersResult(val shouldReplaceResult: Boolean, val isAmbiguousResult: Boolean)

/**
 * Handle special cases when classifiers don't cause ambiguity (`Throws`)
 *
 * The following output options are possible:
 *   * `shouldReplaceResult = true, isAmbiguousResult = false` means successful disambiguation
 *     but the previous result should be replaced with the new one (typically class symbol wins typealias)
 *   * `shouldReplaceResult = false, isAmbiguousResult = false` means successful disambiguation
 *     but the new result should be discarded
 *   * `shouldReplaceResult = false, isAmbiguousResult = true` means unsuccessful disambiguation
 *     and both results become irrelevant
 */
private fun checkUnambiguousClassifiers(
    foundClassifierSymbol: FirClassifierSymbol<*>,
    newClassifierSymbol: FirClassifierSymbol<*>,
    session: FirSession,
): CheckUnambiguousClassifiersResult {
    val classTypealiasesThatDontCauseAmbiguity = session.platformClassMapper.classTypealiasesThatDontCauseAmbiguity

    if (foundClassifierSymbol is FirTypeAliasSymbol && newClassifierSymbol is FirRegularClassSymbol &&
        classTypealiasesThatDontCauseAmbiguity[newClassifierSymbol.classId] == foundClassifierSymbol.classId
    ) {
        return CheckUnambiguousClassifiersResult(shouldReplaceResult = true, isAmbiguousResult = false)
    }

    if (newClassifierSymbol is FirTypeAliasSymbol && foundClassifierSymbol is FirRegularClassSymbol &&
        classTypealiasesThatDontCauseAmbiguity[foundClassifierSymbol.classId] == newClassifierSymbol.classId
    ) {
        return CheckUnambiguousClassifiersResult(shouldReplaceResult = false, isAmbiguousResult = false)
    }

    return CheckUnambiguousClassifiersResult(shouldReplaceResult = false, isAmbiguousResult = true)
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

private fun processConstructors(
    matchedSymbol: FirClassLikeSymbol<*>,
    substitutor: ConeSubstitutor,
    processor: (FirFunctionSymbol<*>) -> Unit,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    constructorFilter: ConstructorFilter
) {
    whileAnalysing(session, matchedSymbol.fir) {
        val scope = when (matchedSymbol) {
            is FirTypeAliasSymbol -> {
                val type = matchedSymbol.resolvedExpandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)
                val basicScope = type.scope(
                    session,
                    bodyResolveComponents.scopeSession,
                    CallableCopyTypeCalculator.DoNothing,
                    requiredMembersPhase = FirResolvePhase.STATUS,
                )

                val outerType = bodyResolveComponents.outerClassManager.outerType(type)

                if (basicScope != null) {
                    TypeAliasConstructorsSubstitutingScope(
                        matchedSymbol,
                        basicScope,
                        outerType
                    )
                } else {
                    null
                }
            }
            is FirClassSymbol -> {
                @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
                val firClass = matchedSymbol.fir as FirClass
                when (firClass.classKind) {
                    ClassKind.INTERFACE -> null
                    else -> firClass.scopeForClass(
                        substitutor,
                        session,
                        bodyResolveComponents.scopeSession,
                        firClass.symbol.toLookupTag(),
                        memberRequiredPhase = FirResolvePhase.STATUS,
                    )
                }
            }
        }

        scope?.processDeclaredConstructors {
            val shouldProcess = when (it.fir.isInner) {
                true -> constructorFilter.acceptInner
                false -> constructorFilter.acceptNested
            }
            if (shouldProcess) {
                processor(it)
            }
        }
    }
}
