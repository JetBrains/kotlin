/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class JavaClassMembersEnhancementScope(
    session: FirSession,
    owner: FirRegularClassSymbol,
    private val useSiteMemberScope: JavaClassUseSiteMemberScope,
) : FirTypeScope() {
    private val overriddenFunctions = mutableMapOf<FirFunctionSymbol<*>, Collection<FirFunctionSymbol<*>>>()
    private val overriddenProperties = mutableMapOf<FirPropertySymbol, Collection<FirPropertySymbol>>()

    private val overrideBindCache = mutableMapOf<Name, Map<FirCallableSymbol<*>?, List<FirCallableSymbol<*>>>>()
    private val signatureEnhancement = FirSignatureEnhancement(owner.fir, session) {
        overriddenMembers(name)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            val enhancedPropertySymbol = signatureEnhancement.enhancedProperty(original, name)

            if (enhancedPropertySymbol is FirPropertySymbol) {
                val enhancedProperty = enhancedPropertySymbol.fir
                overriddenProperties[enhancedPropertySymbol] =
                    enhancedProperty
                        .overriddenMembers(enhancedProperty.name)
                        .mapNotNull { it.symbol as? FirPropertySymbol }
            }

            processor(enhancedPropertySymbol)
        }

        return super.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val enhancedFunctionSymbol = signatureEnhancement.enhancedFunction(original, name)
            val enhancedFunction = enhancedFunctionSymbol.fir as? FirSimpleFunction

            overriddenFunctions[enhancedFunctionSymbol] =
                enhancedFunction
                    ?.overriddenMembers(enhancedFunction.name)
                    ?.mapNotNull { it.symbol as? FirFunctionSymbol<*> }
                    .orEmpty()

            processor(enhancedFunctionSymbol)
        }

        return super.processFunctionsByName(name, processor)
    }

    private fun FirCallableMemberDeclaration<*>.overriddenMembers(name: Name): List<FirCallableMemberDeclaration<*>> {
        val backMap = overrideBindCache.getOrPut(name) {
            useSiteMemberScope.bindOverrides(name)
            useSiteMemberScope
                .overrideByBase
                .toList()
                .groupBy({ (_, key) -> key }, { (value) -> value })
        }
        return backMap[this.symbol]?.map { it.fir as FirCallableMemberDeclaration<*> } ?: emptyList()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        useSiteMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors process@{ original ->
            val function = signatureEnhancement.enhancedFunction(original, name = null)
            processor(function as FirConstructorSymbol)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        doProcessDirectOverriddenCallables(
            functionSymbol, processor, overriddenFunctions, useSiteMemberScope,
            FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = doProcessDirectOverriddenCallables(
        propertySymbol, processor, overriddenProperties, useSiteMemberScope,
        FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
    )

    override fun getCallableNames(): Set<Name> {
        return useSiteMemberScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteMemberScope.getClassifierNames()
    }

    override fun mayContainName(name: Name): Boolean {
        return useSiteMemberScope.mayContainName(name)
    }
}
