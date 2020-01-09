/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

abstract class AbstractFirUseSiteMemberScope(
    session: FirSession,
    overrideChecker: FirOverrideChecker,
    protected val superTypesScope: FirScope,
    protected val declaredMemberScope: FirScope
) : AbstractFirOverrideScope(session, overrideChecker) {

    private val functions = hashMapOf<Name, Collection<FirFunctionSymbol<*>>>()

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
            val symbol = processInheritedDefaultParameters(it)
            overrideCandidates += symbol
            add(symbol)
        }

        superTypesScope.processFunctionsByName(name) {
            val overriddenBy = it.getOverridden(overrideCandidates)
            if (overriddenBy == null) {
                add(it)
            }
        }
    }

    private fun processInheritedDefaultParameters(symbol: FirFunctionSymbol<*>): FirFunctionSymbol<*> {
        val firSimpleFunction = symbol.fir as? FirSimpleFunction ?: return symbol
        if (firSimpleFunction.valueParameters.isEmpty() || firSimpleFunction.valueParameters.any { it.defaultValue != null }) return symbol

        var foundFir: FirFunction<*>? = null
        superTypesScope.processFunctionsByName(symbol.callableId.callableName) { superSymbol ->
            val superFunctionFir = superSymbol.fir
            if (foundFir == null &&
                superFunctionFir is FirSimpleFunction &&
                overrideChecker.isOverriddenFunction(firSimpleFunction, superFunctionFir) &&
                superFunctionFir.valueParameters.any { parameter -> parameter.defaultValue != null }
            ) {
                foundFir = superFunctionFir
            }
        }

        if (foundFir == null) return symbol

        val newSymbol = FirNamedFunctionSymbol(symbol.callableId, false, null)

        createFunctionCopy(firSimpleFunction, newSymbol).apply {
            resolvePhase = firSimpleFunction.resolvePhase
            typeParameters += firSimpleFunction.typeParameters
            valueParameters += firSimpleFunction.valueParameters.zip(foundFir.valueParameters)
                .map { (overrideParameter, overriddenParameter) ->
                    if (overriddenParameter.defaultValue != null)
                        createValueParameterCopy(overrideParameter, overriddenParameter.defaultValue).apply {
                            annotations += overrideParameter.annotations
                        }
                    else
                        overrideParameter
                }
        }

        return newSymbol
    }

    protected open fun createFunctionCopy(firSimpleFunction: FirSimpleFunction, newSymbol: FirNamedFunctionSymbol): FirSimpleFunctionImpl =
        FirSimpleFunctionImpl(
            firSimpleFunction.source,
            firSimpleFunction.session,
            firSimpleFunction.returnTypeRef,
            firSimpleFunction.receiverTypeRef,
            firSimpleFunction.name,
            firSimpleFunction.status,
            newSymbol
        )

    protected open fun createValueParameterCopy(parameter: FirValueParameter, newDefaultValue: FirExpression?): FirValueParameterImpl =
        with(parameter) {
            FirValueParameterImpl(
                source,
                session,
                returnTypeRef,
                name,
                FirVariableSymbol(parameter.symbol.callableId),
                newDefaultValue,
                isCrossinline,
                isNoinline,
                isVararg
            )
        }

    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> Unit
    ) {
        declaredMemberScope.processClassifiersByName(name, processor)
        superTypesScope.processClassifiersByName(name, processor)
    }
}
