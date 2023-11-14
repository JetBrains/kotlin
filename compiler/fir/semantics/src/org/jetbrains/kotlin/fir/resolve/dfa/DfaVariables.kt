/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.types.SmartcastStability

data class Identifier(
    val symbol: FirBasedSymbol<*>,
    val dispatchReceiver: DataFlowVariable?,
    val extensionReceiver: DataFlowVariable?
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
    val isThisReference: Boolean,
    val explicitReceiverVariable: DataFlowVariable?,
    variableIndexForDebug: Int,
    stability: PropertyStability,
) : DataFlowVariable(variableIndexForDebug) {
    val dependentVariables = mutableSetOf<RealVariable>()

    val stability: PropertyStability = stability.combineWithReceiverStability((explicitReceiverVariable as? RealVariable)?.stability)

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    private val _hashCode by lazy {
        31 * identifier.hashCode() + (explicitReceiverVariable?.hashCode() ?: 0)
    }

    override fun hashCode(): Int {
        return _hashCode
    }

    init {
        if (explicitReceiverVariable is RealVariable) {
            explicitReceiverVariable.dependentVariables.add(this)
        }
    }
}

class SyntheticVariable(val fir: FirElement, variableIndexForDebug: Int) : DataFlowVariable(variableIndexForDebug) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SyntheticVariable

        return fir isEqualsTo other.fir
    }

    override fun hashCode(): Int {
        // hack for enums
        return if (fir is FirResolvedQualifier) {
            31 * fir.packageFqName.hashCode() + fir.classId.hashCode()
        } else {
            fir.hashCode()
        }
    }
}

private infix fun FirElement.isEqualsTo(other: FirElement): Boolean {
    if (this !is FirResolvedQualifier || other !is FirResolvedQualifier) return this == other
    if (packageFqName != other.packageFqName) return false
    if (classId != other.classId) return false
    return true
}
