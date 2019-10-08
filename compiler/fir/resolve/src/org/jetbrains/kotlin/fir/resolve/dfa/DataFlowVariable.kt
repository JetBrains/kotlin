/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/*
 * isSynthetic = false for variables that represents actual variables in fir
 * isSynthetic = true for complex expressions (like when expression)
 */
sealed class DataFlowVariable(private val variableIndexForDebug: Int, val fir: FirElement) {
    final override fun toString(): String {
        return "d$variableIndexForDebug"
    }
}

open class RealDataFlowVariable(index: Int, fir: FirElement, val isThisReference: Boolean) : DataFlowVariable(index, fir)

class SyntheticDataFlowVariable(index: Int, fir: FirElement) : DataFlowVariable(index, fir)

class AliasedDataFlowVariable(
    index: Int,
    fir: FirElement,
    var delegate: RealDataFlowVariable
) : RealDataFlowVariable(index, fir, delegate.isThisReference)

// -------------------------------------------------------------------------------------------------------------------------

@UseExperimental(ExperimentalContracts::class)
fun DataFlowVariable.isSynthetic(): Boolean {
    contract {
        returns(true) implies (this@isSynthetic is SyntheticDataFlowVariable)
    }
    return this is SyntheticDataFlowVariable
}

@UseExperimental(ExperimentalContracts::class)
fun DataFlowVariable.isAliasVariable(): Boolean {
    contract {
        returns(true) implies (this@isAliasVariable is AliasedDataFlowVariable)
    }
    return this is AliasedDataFlowVariable
}

val RealDataFlowVariable.variableUnderAlias: RealDataFlowVariable
    get() {
        var variable = this
        while (variable.isAliasVariable()) {
            variable = variable.delegate
        }
        return variable
    }

// -------------------------------------------------------------------------------------------------------------------------

class DataFlowVariableStorage {
    private val fir2DfiMap: MutableMap<FirElement, DataFlowVariable> = mutableMapOf()
    private var debugIndexCounter: Int = 1

    fun getOrCreateNewRealVariable(symbol: AbstractFirBasedSymbol<*>): RealDataFlowVariable {
        return getOrCreateNewRealVariableImpl(symbol, false)
    }

    fun getOrCreateNewThisRealVariable(symbol: AbstractFirBasedSymbol<*>): RealDataFlowVariable {
        return getOrCreateNewRealVariableImpl(symbol, true)
    }

    private fun getOrCreateNewRealVariableImpl(symbol: AbstractFirBasedSymbol<*>, isThisReference: Boolean): RealDataFlowVariable {
        val fir = symbol.fir
        get(fir)?.let { return it as RealDataFlowVariable }
        return RealDataFlowVariable(debugIndexCounter++, fir, isThisReference).also { storeVariable(it, fir) }
    }

    fun getOrCreateNewSyntheticVariable(fir: FirElement): SyntheticDataFlowVariable {
        get(fir)?.let { return it as SyntheticDataFlowVariable }
        return SyntheticDataFlowVariable(debugIndexCounter++, fir).also { storeVariable(it, fir) }
    }

    fun createAliasVariable(symbol: AbstractFirBasedSymbol<*>, variable: RealDataFlowVariable) {
        createAliasVariable(symbol.fir, variable)
    }

    private fun createAliasVariable(fir: FirElement, variable: RealDataFlowVariable) {
        AliasedDataFlowVariable(debugIndexCounter++, fir, variable).also { storeVariable(it, fir) }
    }

    fun rebindAliasVariable(aliasVariable: RealDataFlowVariable, newVariable: RealDataFlowVariable) {
        val fir = removeVariable(aliasVariable)
        requireNotNull(fir)
        createAliasVariable(fir, newVariable)
    }

    fun removeRealVariable(symbol: AbstractFirBasedSymbol<*>) {
        removeSyntheticVariable(symbol.fir)
    }

    fun removeSyntheticVariable(fir: FirElement) {
        val variable = fir2DfiMap[fir] ?: return
        removeVariable(variable)
    }

    fun removeVariable(variable: DataFlowVariable): FirElement? {
        return variable.fir.also {
            fir2DfiMap.remove(it)
        }
    }

    fun removeVariableIfSynthetic(variable: DataFlowVariable): FirElement? {
        if (variable is SyntheticDataFlowVariable) {
            return removeVariable(variable)
        }
        return null
    }

    operator fun get(variable: DataFlowVariable): FirElement? {
        return variable.fir
    }

    operator fun get(firElement: FirElement): DataFlowVariable? {
        return fir2DfiMap[firElement]
    }

    operator fun get(symbol: AbstractFirBasedSymbol<*>): RealDataFlowVariable? {
        return fir2DfiMap[symbol.fir] as RealDataFlowVariable?
    }

    fun reset() {
        fir2DfiMap.clear()
        debugIndexCounter = 1
    }

    private fun storeVariable(variable: DataFlowVariable, fir: FirElement) {
        fir2DfiMap[fir] = variable
    }
}
