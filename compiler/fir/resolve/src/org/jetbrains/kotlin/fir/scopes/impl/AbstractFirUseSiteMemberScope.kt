/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirValueParameterBuilder
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

abstract class AbstractFirUseSiteMemberScope(
    session: FirSession,
    overrideChecker: FirOverrideChecker,
    protected val superTypesScope: FirTypeScope,
    protected val declaredMemberScope: FirScope
) : AbstractFirOverrideScope(session, overrideChecker) {

    private val functions = hashMapOf<Name, Collection<FirFunctionSymbol<*>>>()
    private val directOverridden = hashMapOf<FirFunctionSymbol<*>, Collection<FirFunctionSymbol<*>>>()

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
            this@AbstractFirUseSiteMemberScope.directOverridden[it] = directOverridden
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

    protected open fun createFunctionCopy(
        firSimpleFunction: FirSimpleFunction,
        newSymbol: FirNamedFunctionSymbol
    ): FirSimpleFunctionBuilder =
        FirSimpleFunctionBuilder().apply {
            source = firSimpleFunction.source
            session = firSimpleFunction.session
            origin = FirDeclarationOrigin.FakeOverride
            returnTypeRef = firSimpleFunction.returnTypeRef
            receiverTypeRef = firSimpleFunction.receiverTypeRef
            name = firSimpleFunction.name
            status = firSimpleFunction.status
            symbol = newSymbol
        }

    protected open fun createValueParameterCopy(parameter: FirValueParameter, newDefaultValue: FirExpression?): FirValueParameterBuilder =
        FirValueParameterBuilder().apply {
            source = parameter.source
            session = parameter.session
            origin = FirDeclarationOrigin.FakeOverride
            returnTypeRef = parameter.returnTypeRef
            name = parameter.name
            symbol = FirVariableSymbol(parameter.symbol.callableId)
            defaultValue = newDefaultValue
            isCrossinline = parameter.isCrossinline
            isNoinline = parameter.isNoinline
            isVararg = parameter.isVararg
        }

    override fun processOverriddenFunctionsWithDepth(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction
    ): ProcessorAction = doProcessOverriddenFunctions(functionSymbol, processor, directOverridden, superTypesScope)

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
        superTypesScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        declaredMemberScope.processDeclaredConstructors(processor)
    }
}
