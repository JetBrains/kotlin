/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.initialSignatureAttr
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.syntheticNamesProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class JavaClassMembersEnhancementScope(
    session: FirSession,
    private val owner: FirRegularClassSymbol,
    private val useSiteMemberScope: JavaClassUseSiteMemberScope,
) : FirDelegatingTypeScope(useSiteMemberScope) {
    private val enhancedToOriginalFunctions = mutableMapOf<FirNamedFunctionSymbol, FirNamedFunctionSymbol>()
    private val enhancedToOriginalProperties = mutableMapOf<FirPropertySymbol, FirPropertySymbol>()

    private val signatureEnhancement = FirSignatureEnhancement(owner.fir, session) {
        overriddenMembers()
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            val enhancedPropertySymbol = signatureEnhancement.enhancedProperty(original, name)
            if (original is FirPropertySymbol && enhancedPropertySymbol is FirPropertySymbol) {
                enhancedToOriginalProperties[enhancedPropertySymbol] = original
            }

            processor(enhancedPropertySymbol)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val symbol = signatureEnhancement.enhancedFunction(original, name)
            val enhancedFunction = (symbol.fir as? FirSimpleFunction)
            val enhancedFunctionSymbol = enhancedFunction?.symbol ?: symbol
            enhancedToOriginalFunctions[enhancedFunctionSymbol] = original
            processor(enhancedFunctionSymbol)
        }
    }

    private fun FirCallableDeclaration.overriddenMembers(): List<FirCallableDeclaration> {
        return useSiteMemberScope.getDirectOverriddenMembers(symbol).map { it.fir }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors process@{ original ->
            val function = signatureEnhancement.enhancedConstructor(original)
            processor(function)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        doProcessDirectOverriddenCallables(
            functionSymbol, processor, enhancedToOriginalFunctions, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = doProcessDirectOverriddenCallables(
        propertySymbol, processor, enhancedToOriginalProperties, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
    )

    private fun <S : FirCallableSymbol<*>> doProcessDirectOverriddenCallables(
        callableSymbol: S,
        processor: (S, FirTypeScope) -> ProcessorAction,
        enhancedToOriginalMap: Map<S, S>,
        processDirectOverriddenCallables: FirTypeScope.(S, (S, FirTypeScope) -> ProcessorAction) -> ProcessorAction
    ): ProcessorAction {
        val unwrappedSymbol = if (callableSymbol.origin == FirDeclarationOrigin.RenamedForOverride) {
            @Suppress("UNCHECKED_CAST")
            callableSymbol.fir.initialSignatureAttr as? S ?: callableSymbol
        } else {
            callableSymbol
        }
        val original = enhancedToOriginalMap[unwrappedSymbol] ?: return ProcessorAction.NONE
        return useSiteMemberScope.processDirectOverriddenCallables(original, processor)
    }

    override fun toString(): String {
        return "Java enhancement scope for ${owner.classId}"
    }
}
