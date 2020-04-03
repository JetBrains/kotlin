/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirValueParameterBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
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
            if (it !is FirConstructorSymbol) {
                val overriddenBy = it.getOverridden(overrideCandidates)
                if (overriddenBy == null) {
                    add(it)
                }
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
                        }.build()
                    else
                        overrideParameter
                }
        }.build()

        return newSymbol
    }

    protected open fun createFunctionCopy(firSimpleFunction: FirSimpleFunction, newSymbol: FirNamedFunctionSymbol): FirSimpleFunctionBuilder =
        FirSimpleFunctionBuilder().apply {
            source = firSimpleFunction.source
            session = firSimpleFunction.session
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
            returnTypeRef = parameter.returnTypeRef
            name = parameter.name
            symbol = FirVariableSymbol(parameter.symbol.callableId)
            defaultValue = newDefaultValue
            isCrossinline = parameter.isCrossinline
            isNoinline = parameter.isNoinline
            isVararg = parameter.isVararg
        }


    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> Unit
    ) {
        declaredMemberScope.processClassifiersByName(name, processor)
        superTypesScope.processClassifiersByName(name, processor)
    }
}
