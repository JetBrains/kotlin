/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.types.SmartcastStability
import java.util.*

sealed class DataFlowVariable(private val variableIndexForDebug: Int) : Comparable<DataFlowVariable> {
    final override fun toString(): String {
        return "d$variableIndexForDebug"
    }

    override fun compareTo(other: DataFlowVariable): Int = variableIndexForDebug.compareTo(other.variableIndexForDebug)
}

class RealVariable(
    val symbol: FirBasedSymbol<*>,
    val isReceiver: Boolean,
    val dispatchReceiver: RealVariable?,
    val extensionReceiver: RealVariable?,
    val stability: SmartcastStability,
    variableIndexForDebug: Int,
) : DataFlowVariable(variableIndexForDebug) {
    val dependentVariables = mutableSetOf<RealVariable>()

    override fun equals(other: Any?): Boolean =
        other is RealVariable && symbol == other.symbol &&
                dispatchReceiver == other.dispatchReceiver && extensionReceiver == other.extensionReceiver

    override fun hashCode(): Int =
        Objects.hash(symbol, dispatchReceiver, extensionReceiver)
}

class SyntheticVariable(val fir: FirElement, variableIndexForDebug: Int) : DataFlowVariable(variableIndexForDebug) {
    override fun equals(other: Any?): Boolean =
        other is SyntheticVariable && fir == other.fir

    override fun hashCode(): Int =
        fir.hashCode()
}
