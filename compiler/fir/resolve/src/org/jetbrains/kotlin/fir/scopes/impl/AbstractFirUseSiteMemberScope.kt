/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

abstract class AbstractFirUseSiteMemberScope(
    session: FirSession,
    overrideChecker: FirOverrideChecker,
    protected val superTypesScope: FirTypeScope,
    protected val declaredMemberScope: FirScope
) : AbstractFirOverrideScope(session, overrideChecker) {

    private val functions = hashMapOf<Name, Collection<FirFunctionSymbol<*>>>()
    private val directOverriddenFunctions = hashMapOf<FirFunctionSymbol<*>, Collection<FirFunctionSymbol<*>>>()
    protected val directOverriddenProperties = hashMapOf<FirPropertySymbol, MutableList<FirPropertySymbol>>()

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        functions.getOrPut(name) {
            doProcessFunctions(name)
        }.forEach {
            processor(it)
        }
    }

    private fun doProcessFunctions(
        name: Name
    ): Collection<FirFunctionSymbol<*>> = mutableListOf<FirFunctionSymbol<*>>().apply {
        val overrideCandidates = mutableSetOf<FirFunctionSymbol<*>>()
        declaredMemberScope.processFunctionsByName(name) {
            if (it.isStatic) return@processFunctionsByName
            val directOverridden = computeDirectOverridden(it)
            this@AbstractFirUseSiteMemberScope.directOverriddenFunctions[it] = directOverridden
            val symbol = processInheritedDefaultParameters(it, directOverridden)
            overrideCandidates += symbol
            add(symbol)
        }

        superTypesScope.processFunctionsByName(name) {
            if (it !is FirConstructorSymbol) {
                val overriddenBy = it.getOverridden(overrideCandidates)
                if (overriddenBy == null) {
                    add(it)
                }
            }
        }
    }

    private fun computeDirectOverridden(symbol: FirFunctionSymbol<*>): Collection<FirFunctionSymbol<*>> {
        val result = mutableListOf<FirFunctionSymbol<*>>()
        val firSimpleFunction = symbol.fir as? FirSimpleFunction ?: return emptyList()
        superTypesScope.processFunctionsByName(symbol.callableId.callableName) { superSymbol ->
            val superFunctionFir = superSymbol.fir
            if (superFunctionFir is FirSimpleFunction &&
                overrideChecker.isOverriddenFunction(firSimpleFunction, superFunctionFir)
            ) {
                result.add(superSymbol)
            }
        }

        return result
    }

    private fun processInheritedDefaultParameters(
        symbol: FirFunctionSymbol<*>,
        directOverridden: Collection<FirFunctionSymbol<*>>
    ): FirFunctionSymbol<*> {
        val firSimpleFunction = symbol.fir as? FirSimpleFunction ?: return symbol
        if (firSimpleFunction.valueParameters.isEmpty() || firSimpleFunction.valueParameters.any { it.defaultValue != null }) return symbol

        val overriddenWithDefault: FirFunction<*> =
            directOverridden.singleOrNull {
                it.fir.valueParameters.any { parameter -> parameter.defaultValue != null }
            }?.fir ?: return symbol

        val newSymbol = FirNamedFunctionSymbol(symbol.callableId, false, null)

        createFunctionCopy(firSimpleFunction, newSymbol).apply {
            resolvePhase = firSimpleFunction.resolvePhase
            typeParameters += firSimpleFunction.typeParameters
            valueParameters += firSimpleFunction.valueParameters.zip(overriddenWithDefault.valueParameters)
                .map { (overrideParameter, overriddenParameter) ->
                    if (overriddenParameter.defaultValue != null)
                        createValueParameterCopy(overrideParameter, overriddenParameter.defaultValue).apply {
                            annotations += overrideParameter.annotations
                        }.build()
                    else
                        overrideParameter
                }
        }.build()

        return newSymbol
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        doProcessDirectOverriddenCallables(
            functionSymbol, processor, directOverriddenFunctions, superTypesScope,
            FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        doProcessDirectOverriddenCallables(
            propertySymbol, processor, directOverriddenProperties, superTypesScope,
            FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
        )

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
        superTypesScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        declaredMemberScope.processDeclaredConstructors(processor)
    }

    override fun getCallableNames(): Set<Name> {
        return declaredMemberScope.getContainingCallableNamesIfPresent() + superTypesScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getContainingClassifierNamesIfPresent() + superTypesScope.getClassifierNames()
    }
}
