/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirDelegatedMemberScope(
    private val useSiteScope: FirTypeScope,
    private val session: FirSession
) : FirTypeScope() {
    private val delegatedFunctionCache = mutableMapOf<FirNamedFunctionSymbol, FirNamedFunctionSymbol>()
    private val delegatedPropertyCache = mutableMapOf<FirPropertySymbol, FirPropertySymbol>()

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
                val delegatedFunction = buildSimpleFunction {
                    origin = FirDeclarationOrigin.Delegated
                    session = this@FirDelegatedMemberScope.session
                    this.name = name
                    symbol = FirNamedFunctionSymbol(
                        functionSymbol.callableId,
                        overriddenSymbol = functionSymbol
                    )
                    status = FirDeclarationStatusImpl(original.visibility, Modality.OPEN).apply {
                        isOperator = original.isOperator
                    }
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    returnTypeRef = original.returnTypeRef
                    receiverTypeRef = original.receiverTypeRef
                    valueParameters.addAll(original.valueParameters)
                    typeParameters.addAll(original.typeParameters)
                    annotations.addAll(original.annotations)
                }
                delegatedFunction.symbol as FirNamedFunctionSymbol
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
                val delegatedProperty = buildProperty {
                    origin = FirDeclarationOrigin.Delegated
                    session = this@FirDelegatedMemberScope.session
                    this.name = name
                    symbol = FirPropertySymbol(
                        propertySymbol.callableId,
                        overriddenSymbol = propertySymbol
                    )
                    isVar = original.isVar
                    isLocal = false
                    status = FirDeclarationStatusImpl(original.visibility, Modality.OPEN)
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    returnTypeRef = original.returnTypeRef
                    receiverTypeRef = original.receiverTypeRef
                    typeParameters.addAll(original.typeParameters)
                    annotations.addAll(original.annotations)
                }
                delegatedProperty.symbol
            }
            processor(delegatedSymbol)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return useSiteScope.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return useSiteScope.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor)
    }

    override fun getCallableNames(): Set<Name> {
        return useSiteScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteScope.getClassifierNames()
    }
}