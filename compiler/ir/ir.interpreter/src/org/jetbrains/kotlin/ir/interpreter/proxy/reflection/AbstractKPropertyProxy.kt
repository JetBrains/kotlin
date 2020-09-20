/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.state.KPropertyState
import org.jetbrains.kotlin.ir.types.IrType
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KVisibility

internal abstract class AbstractKPropertyProxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : ReflectionProxy, KProperty<Any?> {

    protected val propertyType: IrType
        get() = state.property.getter!!.returnType

    override val isAbstract: Boolean
        get() = state.property.modality == Modality.ABSTRACT
    override val isConst: Boolean
        get() = state.property.isConst
    override val isFinal: Boolean
        get() = state.property.modality == Modality.FINAL
    override val isLateinit: Boolean
        get() = state.property.isLateinit
    override val isOpen: Boolean
        get() = state.property.modality == Modality.OPEN
    override val isSuspend: Boolean
        get() = false
    override val name: String
        get() = state.property.name.asString()

    override val annotations: List<Annotation>
        get() = TODO("Not yet implemented")
    override val returnType: KType
        get() = TODO("Not yet implemented")
    override val visibility: KVisibility?
        get() = state.irClass.visibility.toKVisibility()

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractKPropertyProxy) return false
        return state == other.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun toString(): String {
        return state.toString()
    }
}