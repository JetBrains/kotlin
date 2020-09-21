/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import kotlin.reflect.*

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
        get() = TODO("not implemented")
    override val parameters: List<KParameter>
        get() = state.getParameters(interpreter)
    override val returnType: KType
        get() = state.getReturnType(interpreter)
    override val typeParameters: List<KTypeParameter>
        get() = listOf()
    override val visibility: KVisibility?
        get() = state.property.visibility.toKVisibility()

    override fun call(vararg args: Any?): Any? = getter.call(*args)

    override fun callBy(args: Map<KParameter, Any?>): Any? = getter.callBy(args)

    protected fun checkArguments(expected: Int, actual: Int) {
        if (expected != actual) {
            throw IllegalArgumentException("Callable expects $expected arguments, but $actual were provided.")
        }
    }

    abstract inner class Getter(val getter: IrSimpleFunction) : KProperty.Getter<Any?> {
        override val property: KProperty<Any?> = this@AbstractKPropertyProxy

        override val name: String = "<get-${this@AbstractKPropertyProxy.name.capitalizeAsciiOnly()}>"
        override val annotations: List<Annotation>
            get() = this@AbstractKPropertyProxy.annotations
        override val parameters: List<KParameter>
            get() = this@AbstractKPropertyProxy.parameters
        override val returnType: KType
            get() = this@AbstractKPropertyProxy.returnType
        override val typeParameters: List<KTypeParameter>
            get() = this@AbstractKPropertyProxy.typeParameters

        override val isInline: Boolean = getter.isInline
        override val isExternal: Boolean = getter.isExternal
        override val isOperator: Boolean = getter.isOperator
        override val isInfix: Boolean = getter.isInfix
        override val isSuspend: Boolean = getter.isSuspend

        override val visibility: KVisibility? = getter.visibility.toKVisibility()
        override val isFinal: Boolean = getter.modality == Modality.FINAL
        override val isOpen: Boolean = getter.modality == Modality.OPEN
        override val isAbstract: Boolean = getter.modality == Modality.ABSTRACT
    }

    abstract inner class Setter(val setter: IrSimpleFunction) : KMutableProperty.Setter<Any?> {
        override val property: KProperty<Any?> = this@AbstractKPropertyProxy

        override val name: String = "<set-${this@AbstractKPropertyProxy.name.capitalizeAsciiOnly()}>"
        override val annotations: List<Annotation>
            get() = this@AbstractKPropertyProxy.annotations
        override val parameters: List<KParameter>
            get() = this@AbstractKPropertyProxy.parameters
        override val returnType: KType
            get() = this@AbstractKPropertyProxy.returnType
        override val typeParameters: List<KTypeParameter>
            get() = this@AbstractKPropertyProxy.typeParameters

        override val isInline: Boolean = setter.isInline
        override val isExternal: Boolean = setter.isExternal
        override val isOperator: Boolean = setter.isOperator
        override val isInfix: Boolean = setter.isInfix
        override val isSuspend: Boolean = setter.isSuspend

        override val visibility: KVisibility? = setter.visibility.toKVisibility()
        override val isFinal: Boolean = setter.modality == Modality.FINAL
        override val isOpen: Boolean = setter.modality == Modality.OPEN
        override val isAbstract: Boolean = setter.modality == Modality.ABSTRACT
    }

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