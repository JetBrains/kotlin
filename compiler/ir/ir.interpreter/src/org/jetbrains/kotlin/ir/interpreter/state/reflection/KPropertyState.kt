/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.interpreter.renderType
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.util.parentAsClass

internal class KPropertyState(
    val property: IrProperty, val dispatchReceiver: State?, val extensionReceiver: State?
) : ReflectionState(property.parentAsClass.symbol) {
    constructor(propertyReference: IrPropertyReference, dispatchReceiver: State?, extensionReceiver: State?)
            : this(propertyReference.symbol.owner, dispatchReceiver, extensionReceiver)

    fun isKProperty0(): Boolean {
        return dispatchReceiver != null && extensionReceiver == null
    }

    fun isKProperty1(): Boolean {
        return dispatchReceiver == null && extensionReceiver == null
    }

    fun isKProperty2(): Boolean {
        return dispatchReceiver != null && extensionReceiver != null
    }

    fun isKMutableProperty0(): Boolean {
        return isKProperty0() && property.isVar
    }

    fun isKMutableProperty1(): Boolean {
        return isKProperty1() && property.isVar
    }

    fun isKMutableProperty2(): Boolean {
        return isKProperty1() && property.isVar
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KPropertyState

        if (property != other.property) return false
        if (dispatchReceiver != other.dispatchReceiver) return false
        if (extensionReceiver != other.extensionReceiver) return false

        return true
    }

    override fun hashCode(): Int {
        var result = property.hashCode()
        result = 31 * result + (dispatchReceiver?.hashCode() ?: 0)
        result = 31 * result + (extensionReceiver?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        val prefix = if (property.isVar) "var" else "val"
        val receivers = renderReceivers(property.getter?.dispatchReceiverParameter?.type, property.getter?.extensionReceiverParameter?.type)
        val returnType = property.getter!!.returnType.renderType()
        return "$prefix $receivers${property.name}: $returnType"
    }
}