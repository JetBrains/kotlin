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

    private val overrideBindCache = mutableMapOf<Name, Map<FirCallableSymbol<*>?, List<FirCallableSymbol<*>>>>()
    private val signatureEnhancement = FirSignatureEnhancement(owner.fir, session) {
        overriddenMembers()
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            processor(signatureEnhancement.enhancedProperty(original, name))
        }

        return super.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val enhancedFunction = signatureEnhancement.enhancedFunction(original, name)

            overriddenFunctions[enhancedFunction] =
                (enhancedFunction.fir as? FirSimpleFunction)
                    ?.overriddenMembers()
                    ?.mapNotNull { it.symbol as? FirFunctionSymbol<*> }
                    .orEmpty()

            processor(enhancedFunction)
        }

        return super.processFunctionsByName(name, processor)
    }

    private fun FirSimpleFunction.overriddenMembers(): List<FirCallableMemberDeclaration<*>> {
        val backMap = overrideBindCache.getOrPut(this.name) {
            useSiteMemberScope.bindOverrides(this.name)
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

    override fun processOverriddenFunctionsWithDepth(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction
    ): ProcessorAction = doProcessOverriddenFunctions(functionSymbol, processor, overriddenFunctions, useSiteMemberScope)
}
