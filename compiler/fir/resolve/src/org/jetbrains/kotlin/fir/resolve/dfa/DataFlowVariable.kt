/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/*
 * isSynthetic = false for variables that represents actual variables in fir
 * isSynthetic = true for complex expressions (like when expression)
 */
sealed class DataFlowVariable(val variableIndexForDebug: Int, val fir: FirElement) {
    abstract val isSynthetic: Boolean
    abstract val aliasedVariable: DataFlowVariable
    abstract val isThisReference: Boolean

    final override fun toString(): String {
        return "d$variableIndexForDebug"
    }
}

private class RealDataFlowVariable(index: Int, fir: FirElement, override val isThisReference: Boolean) : DataFlowVariable(index, fir) {
    override val isSynthetic: Boolean get() = false

    override val aliasedVariable: DataFlowVariable get() = this
}

private class SyntheticDataFlowVariable(index: Int, fir: FirElement) : DataFlowVariable(index, fir) {
    override val isSynthetic: Boolean get() = true

    override val aliasedVariable: DataFlowVariable get() = this

    override val isThisReference: Boolean get() = false
}

private class AliasedDataFlowVariable(index: Int, fir: FirElement, var delegate: DataFlowVariable) : DataFlowVariable(index, fir) {
    override val isSynthetic: Boolean get() = delegate.isSynthetic

    override val aliasedVariable: DataFlowVariable get() = delegate.aliasedVariable

    override val isThisReference: Boolean get() = false
}


class DataFlowVariableStorage {
    private val fir2DfiMap: MutableMap<FirElement, DataFlowVariable> = mutableMapOf()
    private var debugIndexCounter: Int = 1

    fun getOrCreateNewRealVariable(symbol: FirBasedSymbol<*>): DataFlowVariable {
        return getOrCreateNewRealVariableImpl(symbol, false)
    }

    fun getOrCreateNewThisRealVariable(symbol: FirBasedSymbol<*>): DataFlowVariable {
        return getOrCreateNewRealVariableImpl(symbol, true)
    }

    private fun getOrCreateNewRealVariableImpl(symbol: FirBasedSymbol<*>, isThisReference: Boolean): DataFlowVariable {
        val fir = symbol.fir
        get(fir)?.let { return it }
        return RealDataFlowVariable(debugIndexCounter++, fir, isThisReference).also { storeVariable(it, fir) }
    }

    fun getOrCreateNewSyntheticVariable(fir: FirElement): DataFlowVariable {
        get(fir)?.let { return it }
        return SyntheticDataFlowVariable(debugIndexCounter++, fir).also { storeVariable(it, fir) }
    }

    fun createAliasVariable(symbol: FirBasedSymbol<*>, variable: DataFlowVariable) {
        createAliasVariable(symbol.fir, variable)
    }

    private fun createAliasVariable(fir: FirElement, variable: DataFlowVariable) {
        AliasedDataFlowVariable(debugIndexCounter++, fir, variable).also { storeVariable(it, fir) }
    }

    fun rebindAliasVariable(aliasVariable: DataFlowVariable, newVariable: DataFlowVariable) {
        val fir = removeVariable(aliasVariable)
        requireNotNull(fir)
        createAliasVariable(fir, newVariable)
    }

    fun removeRealVariable(symbol: FirBasedSymbol<*>) {
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

    operator fun get(variable: DataFlowVariable): FirElement? {
        return variable.fir
    }

    operator fun get(firElement: FirElement): DataFlowVariable? {
        return fir2DfiMap[firElement]
    }

    operator fun get(symbol: FirBasedSymbol<*>): DataFlowVariable? {
        return fir2DfiMap[symbol.fir]
    }

    fun reset() {
        fir2DfiMap.clear()
        debugIndexCounter = 1
    }

    private fun storeVariable(variable: DataFlowVariable, fir: FirElement) {
        fir2DfiMap[fir] = variable
    }
}
