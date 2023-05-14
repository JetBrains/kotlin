/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.builtins.StandardNames.DYNAMIC_FQ_NAME
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirOperationNameConventions
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.create
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

@RequiresOptIn(message = "Please, don't create FirDynamicScope-s manually")
annotation class FirDynamicScopeConstructor

class FirDynamicScope @FirDynamicScopeConstructor constructor(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
) : FirTypeScope() {
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NEXT

    override fun getCallableNames(): Set<Name> = emptySet()

    override fun getClassifierNames(): Set<Name> = emptySet()

    private val anyTypeScope by lazy {
        session.builtinTypes.anyType.type.scope(
            session,
            scopeSession,
            FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = null,
        )
    }

    override fun processFunctionsByName(
        name: Name,
        processor: (FirNamedFunctionSymbol) -> Unit
    ) {
        var foundMemberInAny = false

        anyTypeScope?.processFunctionsByName(name) {
            foundMemberInAny = true
            processor(it)
        }

        if (foundMemberInAny) {
            return
        }

        session.dynamicMembersStorage.functionsCacheByName.getValue(name, null).also {
            processor(it.symbol)
        }
    }

    override fun processPropertiesByName(
        name: Name,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {
        var foundMemberInAny = false

        anyTypeScope?.processPropertiesByName(name) {
            foundMemberInAny = true
            processor(it)
        }

        if (foundMemberInAny) {
            return
        }

        session.dynamicMembersStorage.propertiesCacheByName.getValue(name, null).also {
            processor(it.symbol)
        }
    }
}

class FirDynamicMembersStorage(val session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    @OptIn(FirDynamicScopeConstructor::class)
    private val dynamicScopeCacheByScope: FirCache<ScopeSession, FirDynamicScope, Nothing?> =
        cachesFactory.createCache { it -> FirDynamicScope(session, it) }

    fun getDynamicScopeFor(scopeSession: ScopeSession) = dynamicScopeCacheByScope.getValue(scopeSession, null)

    val functionsCacheByName: FirCache<Name, FirSimpleFunction, Nothing?> =
        cachesFactory.createCache { name -> buildPseudoFunctionByName(name) }

    val propertiesCacheByName: FirCache<Name, FirProperty, Nothing?> =
        cachesFactory.createCache { name -> buildPseudoPropertyByName(name) }

    private val dynamicTypeRef = buildResolvedTypeRef {
        type = ConeDynamicType.create(session)
    }

    private val anyArrayTypeRef = buildResolvedTypeRef {
        type = ConeClassLikeTypeImpl(
            StandardClassIds.Array.toLookupTag(),
            arrayOf(dynamicTypeRef.coneType),
            isNullable = false
        )
    }

    private fun buildPseudoFunctionByName(name: Name) = buildSimpleFunction {
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            EffectiveVisibility.Public,
        ).apply {
            isInfix = true
            isOperator = true
        }

        this.name = name
        this.symbol = FirNamedFunctionSymbol(CallableId(DYNAMIC_FQ_NAME, this.name))

        moduleData = session.moduleData
        origin = FirDeclarationOrigin.DynamicScope
        resolvePhase = FirResolvePhase.BODY_RESOLVE

        returnTypeRef = if (name in FirOperationNameConventions.ASSIGNMENT_NAMES) {
            session.builtinTypes.unitType
        } else {
            dynamicTypeRef
        }

        val parameter = buildValueParameter {
            moduleData = session.moduleData
            containingFunctionSymbol = this@buildSimpleFunction.symbol
            origin = FirDeclarationOrigin.DynamicScope
            returnTypeRef = anyArrayTypeRef
            this.name = Name.identifier("args")
            this.symbol = FirValueParameterSymbol(this.name)
            isCrossinline = false
            isNoinline = false
            isVararg = true
        }

        valueParameters.add(parameter)
    }

    private fun buildPseudoPropertyByName(name: Name) = buildProperty {
        this.name = name
        this.symbol = FirPropertySymbol(CallableId(DYNAMIC_FQ_NAME, this.name))

        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            EffectiveVisibility.Public,
        )

        moduleData = session.moduleData
        origin = FirDeclarationOrigin.DynamicScope
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        returnTypeRef = dynamicTypeRef
        isVar = true
        isLocal = false
    }
}

val FirSession.dynamicMembersStorage: FirDynamicMembersStorage by FirSession.sessionComponentAccessor()
