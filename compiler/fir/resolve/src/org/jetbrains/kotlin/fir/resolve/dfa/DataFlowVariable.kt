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
sealed class DataFlowVariable(val index: Int) {
    abstract val isSynthetic: Boolean
    abstract val real: DataFlowVariable

    final override fun hashCode(): Int {
        return index
    }

    final override fun equals(other: Any?): Boolean {
        if (other !is DataFlowVariable) return false
        return index == other.index
    }

    final override fun toString(): String {
        return "d$index"
    }
}

private class RealDataFlowVariable(index: Int) : DataFlowVariable(index) {
    override val isSynthetic: Boolean get() = false

    override val real: DataFlowVariable get() = this
}

private class SyntheticDataFlowVariable(index: Int) : DataFlowVariable(index) {
    override val isSynthetic: Boolean get() = true

    override val real: DataFlowVariable get() = this
}

private class AliasedDataFlowVariable(index: Int, var delegate: DataFlowVariable) : DataFlowVariable(index) {
    override val isSynthetic: Boolean get() = delegate.isSynthetic

    override val real: DataFlowVariable get() = delegate.real
}


class DataFlowVariableStorage {
    private val dfi2FirMap: MutableMap<DataFlowVariable, FirElement> = mutableMapOf()
    private val fir2DfiMap: MutableMap<FirElement, DataFlowVariable> = mutableMapOf()
    private var counter: Int = 1

    fun getOrCreateNewRealVariable(symbol: FirBasedSymbol<*>): DataFlowVariable {
        val fir = symbol.fir
        get(fir)?.let { return it }
        return RealDataFlowVariable(counter++).also { storeVariable(it, fir) }
    }

    fun getOrCreateNewSyntheticVariable(fir: FirElement): DataFlowVariable {
        get(fir)?.let { return it }
        return SyntheticDataFlowVariable(counter++).also { storeVariable(it, fir) }
    }

    fun createAliasVariable(symbol: FirBasedSymbol<*>, variable: DataFlowVariable) {
        createAliasVariable(symbol.fir, variable)
    }

    private fun createAliasVariable(fir: FirElement, variable: DataFlowVariable) {
        AliasedDataFlowVariable(counter++, variable).also { storeVariable(it, fir) }
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
        return dfi2FirMap.remove(variable)?.also {
            fir2DfiMap.remove(it)
        }
    }

    operator fun get(variable: DataFlowVariable): FirElement? {
        return dfi2FirMap[variable]
    }

    operator fun get(firElement: FirElement): DataFlowVariable? {
        return fir2DfiMap[firElement]
    }

    operator fun get(symbol: FirBasedSymbol<*>): DataFlowVariable? {
        return fir2DfiMap[symbol.fir]
    }

    fun reset() {
        dfi2FirMap.clear()
        fir2DfiMap.clear()
        counter = 1
    }

    private fun storeVariable(variable: DataFlowVariable, fir: FirElement) {
        dfi2FirMap[variable] = fir
        fir2DfiMap[fir] = variable
    }

    @Deprecated("only for debug")
    fun getByIndex(index: Int): DataFlowVariable? = dfi2FirMap.keys.firstOrNull { it.index == index }
}
