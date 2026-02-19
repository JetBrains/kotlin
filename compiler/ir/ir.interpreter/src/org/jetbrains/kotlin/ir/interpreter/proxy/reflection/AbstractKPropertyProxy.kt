/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KFunctionState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import kotlin.reflect.*

internal abstract class AbstractKPropertyProxy(
    override val state: KPropertyState,
    override val callInterceptor: CallInterceptor,
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
        get() = state.getParameters(callInterceptor)
    override val returnType: KType
        get() = state.getReturnType(callInterceptor)
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

    abstract inner class Accessor<R>(
        state: KFunctionState,
    ) : KFunctionProxy<R>(state, this@AbstractKPropertyProxy.callInterceptor), KProperty.Accessor<Any?>, KFunction<R> {
        override val property: KProperty<Any?> = this@AbstractKPropertyProxy
    }

    abstract inner class Getter(getterState: KFunctionState) : Accessor<Any?>(getterState), KProperty.Getter<Any?> {}

    abstract inner class Setter(setterState: KFunctionState) : Accessor<Unit>(setterState), KMutableProperty.Setter<Any?> {}

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