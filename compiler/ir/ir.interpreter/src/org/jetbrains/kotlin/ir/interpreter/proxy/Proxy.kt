/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy

import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.fqName
import org.jetbrains.kotlin.ir.interpreter.isPrimitiveArray
import org.jetbrains.kotlin.ir.interpreter.proxy.CommonProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.ReflectionProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.reflection.ReflectionState
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.name.Name
import java.lang.invoke.MethodType

internal interface Proxy {
    val state: State
    val callInterceptor: CallInterceptor
    val environment
        get() = callInterceptor.environment

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

internal fun State.wrap(callInterceptor: CallInterceptor, remainArraysAsIs: Boolean, extendFrom: Class<*>? = null): Any? {
    return when (this) {
        is ExceptionState -> this
        is Wrapper -> this.value
        is Primitive<*> -> when {
            this.isNull() -> null
            this.type.isArray() || this.type.isPrimitiveArray() -> if (remainArraysAsIs) this else this.value
            else -> this.value
        }
        is Common -> this.asProxy(callInterceptor, extendFrom)
        is ReflectionState -> this.asProxy(callInterceptor)
        else -> throw AssertionError("${this::class} is unsupported as argument for wrap function")
    }
}

private val eqeqName = IrBuiltIns.KOTLIN_INTERNAL_IR_FQN.child(Name.identifier(BuiltInOperatorNames.EQEQ)).asString()
private val checkNotNullName = IrBuiltIns.KOTLIN_INTERNAL_IR_FQN.child(Name.identifier(BuiltInOperatorNames.CHECK_NOT_NULL)).asString()

/**
 * Prepare state object to be passed in outer world
 */
internal fun List<State>.wrap(callInterceptor: CallInterceptor, irFunction: IrFunction, methodType: MethodType? = null): List<Any?> {
    val name = irFunction.fqName
    if (name == eqeqName && this.any { it is Common }) {
        // in case of custom `equals` it is important not to lose information obout type
        // so all states remain as is, only common will be converted to Proxy
        return mapIndexed { index, state ->
            if (state !is Common) return@mapIndexed state
            state.wrap(callInterceptor, remainArraysAsIs = true, methodType?.parameterType(index))
        }
    }
    return this.mapIndexed { index, state ->
        // don't get arrays from Primitive in case of "set" and "Pair.<init>"; information about type will be lost
        val unwrapArrays = (name == "kotlin.Array.set" && index != 0) || name == "kotlin.Pair.<init>" || name == checkNotNullName
        state.wrap(callInterceptor, unwrapArrays, methodType?.parameterType(index))
    }
}

internal fun Class<*>.isObject(): Boolean {
    return this == java.lang.Object::class.java
}