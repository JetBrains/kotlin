/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class FirDelegatedMemberScope(
    private val useSiteScope: FirTypeScope,
    private val session: FirSession,
    private val containingClass: FirClass<*>,
    private val delegateField: FirField,
) : FirTypeScope() {
    private val delegatedFunctionCache = mutableMapOf<FirNamedFunctionSymbol, FirNamedFunctionSymbol>()
    private val delegatedPropertyCache = mutableMapOf<FirPropertySymbol, FirPropertySymbol>()
    private val dispatchReceiverType = containingClass.defaultType()

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteScope.processFunctionsByName(name) processor@{ functionSymbol ->
            if (functionSymbol !is FirNamedFunctionSymbol) {
                processor(functionSymbol)
                return@processor
            }
            val original = functionSymbol.fir
            if (original.modality == Modality.FINAL) {
                processor(functionSymbol)
                return@processor
            }
            val delegatedSymbol = delegatedFunctionCache.getOrPut(functionSymbol) {
                val newSymbol = FirNamedFunctionSymbol(
                    functionSymbol.callableId,
                )
                FirFakeOverrideGenerator.createCopyForFirFunction(
                    newSymbol,
                    original,
                    session,
                    FirDeclarationOrigin.Delegated,
                    newDispatchReceiverType = dispatchReceiverType,
                    newModality = Modality.OPEN,
                ).apply {
                    delegatedWrapperData = DelegatedWrapperData(functionSymbol.fir, containingClass.symbol.toLookupTag(), delegateField)
                }.symbol as FirNamedFunctionSymbol
            }
            processor(delegatedSymbol)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteScope.processPropertiesByName(name) processor@{ propertySymbol ->
            if (propertySymbol !is FirPropertySymbol) {
                processor(propertySymbol)
                return@processor
            }
            val original = propertySymbol.fir
            if (original.modality == Modality.FINAL) {
                processor(propertySymbol)
                return@processor
            }
            val delegatedSymbol = delegatedPropertyCache.getOrPut(propertySymbol) {
                FirFakeOverrideGenerator.createCopyForFirProperty(
                    FirPropertySymbol(
                        propertySymbol.callableId,
                        overriddenSymbol = propertySymbol
                    ),
                    original,
                    session,
                    newModality = Modality.OPEN,
                    newDispatchReceiverType = dispatchReceiverType,
                ).apply {
                    delegatedWrapperData = DelegatedWrapperData(propertySymbol.fir, containingClass.symbol.toLookupTag(), delegateField)
                }.symbol
            }
            processor(delegatedSymbol)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processDirectOverriddenWithBaseScope(
            functionSymbol, processor, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processDirectOverriddenWithBaseScope(
            propertySymbol, processor, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
        )
    }

    private inline fun <reified D : FirCallableSymbol<*>> processDirectOverriddenWithBaseScope(
        symbol: D,
        noinline processor: (D, FirTypeScope) -> ProcessorAction,
        processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(D, ((D, FirTypeScope) -> ProcessorAction)) -> ProcessorAction,
    ): ProcessorAction {
        val wrappedData = (symbol.fir as? FirCallableMemberDeclaration<*>)?.delegatedWrapperData
        return when {
            wrappedData == null || wrappedData.containingClass != containingClass.symbol.toLookupTag() -> {
                useSiteScope.processDirectOverriddenCallablesWithBaseScope(symbol, processor)
            }
            else -> processor(wrappedData.wrapped.symbol as D, useSiteScope)
        }
    }

    override fun getCallableNames(): Set<Name> {
        return useSiteScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteScope.getClassifierNames()
    }
}

private object DelegatedWrapperDataKey : FirDeclarationDataKey()
class DelegatedWrapperData<D : FirCallableDeclaration<*>>(
    val wrapped: D,
    val containingClass: ConeClassLikeLookupTag,
    val delegateField: FirField,
)
var <D : FirCallableDeclaration<*>>
        D.delegatedWrapperData: DelegatedWrapperData<D>? by FirDeclarationDataRegistry.data(DelegatedWrapperDataKey)
