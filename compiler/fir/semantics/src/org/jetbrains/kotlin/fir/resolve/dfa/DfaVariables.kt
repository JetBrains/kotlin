/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.types.SmartcastStability

data class Identifier constructor(
    val symbol: FirBasedSymbol<*>,
    val isReceiver: Boolean,
    val dispatchReceiver: RealVariable?,
    val extensionReceiver: RealVariable?
) {
    override fun toString(): String {
        val callableId = (symbol as? FirCallableSymbol<*>)?.callableId
        return "[$callableId, dispatchReceiver = $dispatchReceiver, extensionReceiver = $extensionReceiver]"
    }
}

sealed class DataFlowVariable(private val variableIndexForDebug: Int) : Comparable<DataFlowVariable> {
    final override fun toString(): String {
        return "d$variableIndexForDebug"
    }

    override fun compareTo(other: DataFlowVariable): Int = variableIndexForDebug.compareTo(other.variableIndexForDebug)
}

enum class PropertyStability(val impliedSmartcastStability: SmartcastStability?) {
    // Immutable and no custom getter or local.
    // Smartcast is definitely safe regardless of usage.
    STABLE_VALUE(SmartcastStability.STABLE_VALUE),

    // Smartcast may or may not be safe, depending on whether there are concurrent writes to this local variable.
    LOCAL_VAR(null),

    // Smartcast is always unsafe regardless of usage.
    EXPECT_PROPERTY(SmartcastStability.EXPECT_PROPERTY),

    // Open or custom getter.
    // Smartcast is always unsafe regardless of usage.
    PROPERTY_WITH_GETTER(SmartcastStability.PROPERTY_WITH_GETTER),

    // Protected / public member value from another module.
    // Smartcast is always unsafe regardless of usage.
    ALIEN_PUBLIC_PROPERTY(SmartcastStability.ALIEN_PUBLIC_PROPERTY),

    // Mutable member property of a class or object.
    // Smartcast is always unsafe regardless of usage.
    MUTABLE_PROPERTY(SmartcastStability.MUTABLE_PROPERTY),

    // Delegated property of a class or object.
    // Smartcast is always unsafe regardless of usage.
    DELEGATED_PROPERTY(SmartcastStability.DELEGATED_PROPERTY);

    fun combineWithReceiverStability(receiverStability: PropertyStability?): PropertyStability {
        if (receiverStability == null) return this
        if (this == LOCAL_VAR) {
            require(receiverStability == STABLE_VALUE || receiverStability == LOCAL_VAR) {
                "LOCAL_VAR can have only stable or local receiver, but got $receiverStability"
            }
            return this
        }
        return maxOf(this, receiverStability)
    }
}

class RealVariable(
    val identifier: Identifier,
    val stability: PropertyStability,
    variableIndexForDebug: Int,
) : DataFlowVariable(variableIndexForDebug) {
    val dependentVariables = mutableSetOf<RealVariable>()

    override fun equals(other: Any?): Boolean {
        return other is RealVariable && identifier == other.identifier
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}

class SyntheticVariable(val fir: FirElement, variableIndexForDebug: Int) : DataFlowVariable(variableIndexForDebug) {
    override fun equals(other: Any?): Boolean =
        other is SyntheticVariable && fir == other.fir

    override fun hashCode(): Int =
        fir.hashCode()
}
