/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructorCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.scopes.scopeForClass
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.types.withReplacedConeType
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.fir.whileAnalysing
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

private operator fun <T> Pair<T, *>?.component1() = this?.first
private operator fun <T> Pair<*, T>?.component2() = this?.second

internal fun FirScope.processConstructorsByName(
    callInfo: CallInfo,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    includeInnerConstructors: Boolean,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    val (matchedClassifierSymbol, substitutor) = getFirstClassifierOrNull(callInfo, session, bodyResolveComponents) ?: return
    val matchedClassSymbol = matchedClassifierSymbol as? FirClassLikeSymbol<*> ?: return

    processConstructors(
        matchedClassSymbol,
        substitutor,
        processor,
        session,
        bodyResolveComponents,
        includeInnerConstructors
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
    includeInnerConstructors: Boolean,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    processConstructorsByName(
        callInfo, session, bodyResolveComponents,
        includeInnerConstructors = includeInnerConstructors,
        processor
    )

    processFunctionsByName(callInfo.name, processor)
}

private data class SymbolWithSubstitutor(val symbol: FirClassifierSymbol<*>, val substitutor: ConeSubstitutor)

fun FirScope.getSingleVisibleClassifier(
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents,
    name: Name
): FirClassifierSymbol<*>? = mutableSetOf<FirClassifierSymbol<*>>().apply {
    processClassifiersByName(name) { classifierSymbol ->
        if (!classifierSymbol.fir.isInvisibleOrHidden(session, bodyResolveComponents)) {
            this.add(classifierSymbol)
        }
    }
}.singleOrNull()

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

    val deprecation = symbol.getDeprecationForCallSite(session.languageVersionSettings.apiVersion)
    return deprecation != null && deprecation.deprecationLevel == DeprecationLevelValue.HIDDEN
}

private fun FirScope.getFirstClassifierOrNull(
    callInfo: CallInfo,
    session: FirSession,
    bodyResolveComponents: BodyResolveComponents
): SymbolWithSubstitutor? {
    var isSuccessResult = false
    var isAmbiguousResult = false
    var result: SymbolWithSubstitutor? = null
    processClassifiersByNameWithSubstitution(callInfo.name) { symbol, substitutor ->
        val classifierDeclaration = symbol.fir
        val isSuccessCandidate = !classifierDeclaration.isInvisibleOrHidden(session, bodyResolveComponents)

        when {
            isSuccessCandidate && !isSuccessResult -> {
                // successful result is better than unsuccessful
                isSuccessResult = true
                isAmbiguousResult = false
                result = SymbolWithSubstitutor(symbol, substitutor)
            }
            result?.symbol === symbol -> {
                // miss identical results
                return@processClassifiersByNameWithSubstitution
            }
            result != null -> {
                if (isSuccessResult == isSuccessCandidate) {
                    // results are similar => ambiguity
                    isAmbiguousResult = true
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

    return result.takeUnless { isAmbiguousResult }
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
    includeInnerConstructors: Boolean
) {
    whileAnalysing(session, matchedSymbol.fir) {
        val scope = when (matchedSymbol) {
            is FirTypeAliasSymbol -> {
                matchedSymbol.lazyResolveToPhase(FirResolvePhase.TYPES)
                val type = matchedSymbol.fir.expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>().fullyExpandedType(session)
                val basicScope = type.scope(
                    session,
                    bodyResolveComponents.scopeSession,
                    FakeOverrideTypeCalculator.DoNothing,
                    requiredPhase = FirResolvePhase.STATUS
                )

                val outerType = bodyResolveComponents.outerClassManager.outerType(type)

                if (basicScope != null &&
                    (matchedSymbol.fir.typeParameters.isNotEmpty() || outerType != null || type.typeArguments.isNotEmpty())
                ) {
                    TypeAliasConstructorsSubstitutingScope(
                        matchedSymbol,
                        basicScope,
                        outerType
                    )
                } else basicScope
            }
            is FirClassSymbol -> {
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

        //TODO: why don't we use declared member scope at this point?
        scope?.processDeclaredConstructors {
            if (includeInnerConstructors || !it.fir.isInner) {
                processor(it)
            }
        }
    }
}

private class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val delegatingScope: FirScope,
    private val outerType: ConeClassLikeType?,
) : FirScope() {

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors wrapper@{ originalConstructorSymbol ->
            val typeParameters = typeAliasSymbol.fir.typeParameters

            processor(
                buildConstructorCopy(originalConstructorSymbol.fir) {
                    symbol = FirConstructorSymbol(originalConstructorSymbol.callableId)
                    origin = FirDeclarationOrigin.Synthetic

                    this.typeParameters.clear()
                    this.typeParameters += typeParameters.map { buildConstructedClassTypeParameterRef { symbol = it.symbol } }

                    if (outerType != null) {
                        // If the matched symbol is a type alias, and the expanded type is a nested class, e.g.,
                        //
                        //   class Outer {
                        //     inner class Inner
                        //   }
                        //   typealias OI = Outer.Inner
                        //   fun foo() { Outer().OI() }
                        //
                        // the chances are that `processor` belongs to [ScopeTowerLevel] (to resolve type aliases at top-level), which treats
                        // the explicit receiver (`Outer()`) as an extension receiver, whereas the constructor of the nested class may regard
                        // the same explicit receiver as a dispatch receiver (hence inconsistent receiver).
                        // Here, we add a copy of the nested class constructor, along with the outer type as an extension receiver, so that it
                        // can be seen as if resolving:
                        //
                        //   fun Outer.OI(): OI = ...
                        //
                        //
                        receiverParameter = originalConstructorSymbol.fir.returnTypeRef.withReplacedConeType(outerType).let {
                            buildReceiverParameter {
                                typeRef = it
                            }
                        }
                    }

                }.apply {
                    originalConstructorIfTypeAlias = originalConstructorSymbol.fir
                }.symbol
            )
        }
    }
}
