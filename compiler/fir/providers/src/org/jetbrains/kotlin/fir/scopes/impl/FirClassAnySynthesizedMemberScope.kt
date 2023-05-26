/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.scopeForSupertype
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBooleanTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitIntTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * This declared scope wrapper is created for data/value classes and provides Any method stubs, if necessary
 */
class FirClassAnySynthesizedMemberScope(
    session: FirSession,
    private val declaredMemberScope: FirContainingNamesAwareScope,
    klass: FirRegularClass,
    scopeSession: ScopeSession,
) : FirContainingNamesAwareScope() {
    private val lookupTag = klass.symbol.toLookupTag()

    private val baseModuleData = klass.moduleData

    private val dispatchReceiverType = klass.defaultType()

    private val synthesizedCache = session.synthesizedStorage.synthesizedCacheByScope.getValue(lookupTag, null)

    private val synthesizedSource = klass.source?.fakeElement(KtFakeSourceElementKind.DataClassGeneratedMembers)

    private val superKlassScope = lookupSuperTypes(
        klass, lookupInterfaces = false, deep = false, useSiteSession = session, substituteTypes = true
    ).firstOrNull()?.scopeForSupertype(session, scopeSession, klass, memberRequiredPhase = FirResolvePhase.TYPES)

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        declaredMemberScope.processDeclaredConstructors(processor)
    }

    override fun getCallableNames(): Set<Name> {
        return declaredMemberScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getClassifierNames()
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        declaredMemberScope.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name !in ANY_MEMBER_NAMES) {
            declaredMemberScope.processFunctionsByName(name, processor)
            return
        }
        var synthesizedFunctionIsNeeded = true
        declaredMemberScope.processFunctionsByName(name) process@{ fromDeclaredScope ->
            if (fromDeclaredScope.matchesSomeAnyMember(name)) {
                // TODO: should we handle fromDeclaredScope.origin == FirDeclarationOrigin.Delegated somehow?
                // See also KT-58926
                synthesizedFunctionIsNeeded = false
            }
            processor(fromDeclaredScope)
        }
        if (!synthesizedFunctionIsNeeded) return
        superKlassScope?.processFunctionsByName(name) { fromSuperType ->
            if (synthesizedFunctionIsNeeded) {
                if (fromSuperType.rawStatus.modality == Modality.FINAL && fromSuperType.matchesSomeAnyMember(name)) {
                    synthesizedFunctionIsNeeded = false
                }
            }
        }
        if (!synthesizedFunctionIsNeeded) return
        processor(synthesizedCache.synthesizedFunction.getValue(name, this))
    }

    private fun FirNamedFunctionSymbol.matchesSomeAnyMember(name: Name): Boolean {
        return when (name) {
            OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING -> {
                valueParameterSymbols.isEmpty() && !isExtension && fir.contextReceivers.isEmpty()
            }
            else -> {
                lazyResolveToPhase(FirResolvePhase.TYPES)
                fir.isEquals()
            }
        }
    }

    internal fun generateSyntheticFunctionByName(name: Name): FirNamedFunctionSymbol =
        when (name) {
            OperatorNameConventions.EQUALS -> generateEqualsFunction()
            OperatorNameConventions.HASH_CODE -> generateHashCodeFunction()
            OperatorNameConventions.TO_STRING -> generateToStringFunction()
            else -> shouldNotBeCalled()
        }.symbol

    private fun generateEqualsFunction(): FirSimpleFunction =
        buildSimpleFunction {
            generateSyntheticFunction(OperatorNameConventions.EQUALS, isOperator = true)
            returnTypeRef = FirImplicitBooleanTypeRef(source)
            this.valueParameters.add(
                buildValueParameter {
                    this.name = Name.identifier("other")
                    origin = FirDeclarationOrigin.Synthetic
                    moduleData = baseModuleData
                    this.returnTypeRef = FirImplicitNullableAnyTypeRef(null)
                    this.symbol = FirValueParameterSymbol(this.name)
                    containingFunctionSymbol = this@buildSimpleFunction.symbol
                    isCrossinline = false
                    isNoinline = false
                    isVararg = false
                }
            )
        }

    private fun generateHashCodeFunction(): FirSimpleFunction =
        buildSimpleFunction {
            generateSyntheticFunction(OperatorNameConventions.HASH_CODE)
            returnTypeRef = FirImplicitIntTypeRef(source)
        }

    private fun generateToStringFunction(): FirSimpleFunction =
        buildSimpleFunction {
            generateSyntheticFunction(OperatorNameConventions.TO_STRING)
            returnTypeRef = FirImplicitStringTypeRef(source)
        }

    private fun FirSimpleFunctionBuilder.generateSyntheticFunction(
        name: Name,
        isOperator: Boolean = false,
    ) {
        this.source = synthesizedSource
        moduleData = baseModuleData
        origin = FirDeclarationOrigin.Synthetic
        this.name = name
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.OPEN, EffectiveVisibility.Public).apply {
            this.isOperator = isOperator
        }
        symbol = FirNamedFunctionSymbol(CallableId(lookupTag.classId, name))
        dispatchReceiverType = this@FirClassAnySynthesizedMemberScope.dispatchReceiverType
    }

    companion object {
        private val ANY_MEMBER_NAMES = hashSetOf(
            OperatorNameConventions.HASH_CODE, OperatorNameConventions.EQUALS, OperatorNameConventions.TO_STRING
        )
    }
}

class FirSynthesizedStorage(val session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    val synthesizedCacheByScope: FirCache<ConeClassLikeLookupTag, SynthesizedCache, Nothing?> =
        cachesFactory.createCache { _ -> SynthesizedCache(session.firCachesFactory) }

    class SynthesizedCache(cachesFactory: FirCachesFactory) {
        val synthesizedFunction: FirCache<Name, FirNamedFunctionSymbol, FirClassAnySynthesizedMemberScope> =
            cachesFactory.createCache { name, scope -> scope.generateSyntheticFunctionByName(name) }
    }
}

private val FirSession.synthesizedStorage: FirSynthesizedStorage by FirSession.sessionComponentAccessor()
