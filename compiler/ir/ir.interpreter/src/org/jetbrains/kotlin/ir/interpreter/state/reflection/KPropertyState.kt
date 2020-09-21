/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.types.classOrNull
import kotlin.reflect.KType

internal class KPropertyState(
    val property: IrProperty, override val irClass: IrClass, val dispatchReceiver: State? = null, val extensionReceiver: State? = null
) : ReflectionState() {

    constructor(propertyReference: IrPropertyReference, dispatchReceiver: State?, extensionReceiver: State?)
            : this(propertyReference.symbol.owner, propertyReference.type.classOrNull!!.owner, dispatchReceiver, extensionReceiver)

    private var _returnType: KType? = null

    fun getReturnType(interpreter: IrInterpreter): KType {
        if (_returnType != null) return _returnType!!
        val kTypeIrClass = irClass.getIrClassOfReflection("returnType")
        _returnType = KTypeProxy(KTypeState(property.getter!!.returnType, kTypeIrClass), interpreter)
        return _returnType!!
    }

    fun isKProperty0(): Boolean = irClass.name.asString() == "KProperty0"

    fun isKProperty1(): Boolean = irClass.name.asString() == "KProperty1"

    fun isKProperty2(): Boolean = irClass.name.asString() == "KProperty2"

    fun isKMutableProperty0(): Boolean = irClass.name.asString() == "KMutableProperty0"

    fun isKMutableProperty1(): Boolean = irClass.name.asString() == "KMutableProperty1"

    fun isKMutableProperty2(): Boolean = irClass.name.asString() == "KMutableProperty2"

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
        return renderProperty(property)
    }
}