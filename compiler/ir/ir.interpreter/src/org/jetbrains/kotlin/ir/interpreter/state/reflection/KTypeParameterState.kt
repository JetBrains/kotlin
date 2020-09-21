/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import kotlin.reflect.KType

internal class KTypeParameterState(val irTypeParameter: IrTypeParameter, override val irClass: IrClass) : ReflectionState() {
    private var _upperBounds: List<KType>? = null

    fun getUpperBounds(interpreter: IrInterpreter): List<KType> {
        if (_upperBounds != null) return _upperBounds!!
        val kTypeIrClass = irClass.getIrClassOfReflectionFromList("upperBounds")
        _upperBounds = irTypeParameter.superTypes.map { KTypeProxy(KTypeState(it, kTypeIrClass), interpreter) }
        return _upperBounds!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KTypeParameterState

        if (irTypeParameter != other.irTypeParameter) return false

        return true
    }

    override fun hashCode(): Int {
        return irTypeParameter.hashCode()
    }

    override fun toString(): String {
        return irTypeParameter.name.asString()
    }
}