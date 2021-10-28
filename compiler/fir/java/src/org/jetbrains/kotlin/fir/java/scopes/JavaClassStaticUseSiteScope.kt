/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isStatic
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

class JavaClassStaticUseSiteScope internal constructor(
    session: FirSession,
    private val declaredMemberScope: FirContainingNamesAwareScope,
    private val superClassScope: FirContainingNamesAwareScope,
    private val superTypesScopes: List<FirContainingNamesAwareScope>,
    javaTypeParameterStack: JavaTypeParameterStack,
) : FirContainingNamesAwareScope() {
    private val functions = hashMapOf<Name, Collection<FirNamedFunctionSymbol>>()
    private val properties = hashMapOf<Name, Collection<FirVariableSymbol<*>>>()
    private val overrideChecker = JavaOverrideChecker(session, javaTypeParameterStack, baseScope = null, considerReturnTypeKinds = false)

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        functions.getOrPut(name) {
            computeFunctions(name)
        }.forEach(processor)
    }

    private fun computeFunctions(name: Name): MutableList<FirNamedFunctionSymbol> {
        val superClassSymbols = mutableListOf<FirNamedFunctionSymbol>()
        superClassScope.processFunctionsByName(name) {
            superClassSymbols.addIfNotNull(it as? FirNamedFunctionSymbol)
        }

        val result = mutableListOf<FirNamedFunctionSymbol>()

        declaredMemberScope.processFunctionsByName(name) l@{ functionSymbol ->
            if (!functionSymbol.isStatic) return@l

            result.add(functionSymbol)
            superClassSymbols.removeAll { superClassSymbol ->
                overrideChecker.isOverriddenFunction(functionSymbol.fir, superClassSymbol.fir)
            }
        }

        result += superClassSymbols

        return result
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return properties.getOrPut(name) {
            computeProperties(name)
        }.forEach(processor)

    }

    private fun computeProperties(name: Name): MutableList<FirVariableSymbol<*>> {
        val result: MutableList<FirVariableSymbol<*>> = mutableListOf()
        declaredMemberScope.processPropertiesByName(name) l@{ propertySymbol ->
            if (!propertySymbol.isStatic) return@l
            result.add(propertySymbol)
        }

        if (result.isNotEmpty()) return result

        for (superTypesScope in superTypesScopes) {
            superTypesScope.processPropertiesByName(name) l@{ propertySymbol ->
                if (!propertySymbol.isStatic) return@l
                result.add(propertySymbol)
            }
        }

        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getCallableNames(): Set<Name> {
        return buildSet {
            addAll(declaredMemberScope.getCallableNames())
            for (superTypesScope in superTypesScopes) {
                addAll(superTypesScope.getCallableNames())
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getClassifierNames(): Set<Name> {
        return buildSet {
            addAll(declaredMemberScope.getClassifierNames())
            for (superTypesScope in superTypesScopes) {
                addAll(superTypesScope.getClassifierNames())
            }
        }
    }

    override fun mayContainName(name: Name): Boolean {
        return declaredMemberScope.mayContainName(name) || superTypesScopes.any { it.mayContainName(name) }
    }

    override val scopeOwnerLookupNames: List<String>
        get() = declaredMemberScope.scopeOwnerLookupNames
}
