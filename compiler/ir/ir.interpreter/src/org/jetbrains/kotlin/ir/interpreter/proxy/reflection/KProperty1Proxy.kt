/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeState
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1

internal open class KProperty1Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : AbstractKPropertyProxy(state, callInterceptor), KProperty1<Any?, Any?> {
    protected fun IrValueParameter.getActualType(): IrType {
        return when (this.type.classOrNull) {
            null -> (state.getField(this.type.classifierOrFail) as KTypeState).irType
            else -> this.type
        }
    }

    override val getter: KProperty1.Getter<Any?, Any?>
        get() = object : Getter(state.property.getter!!), KProperty1.Getter<Any?, Any?> {
            override fun invoke(p1: Any?): Any? = call(p1)

            override fun call(vararg args: Any?): Any? {
                checkArguments(1, args.size)
                val receiverParameter = (getter.dispatchReceiverParameter ?: getter.extensionReceiverParameter)!!
                val receiver = environment.convertToState(args[0], receiverParameter.getActualType())
                return callInterceptor.interceptProxy(getter, listOf(receiver))
            }

            override fun callBy(args: Map<KParameter, Any?>): Any? {
                TODO("Not yet implemented")
            }
        }

    override fun get(receiver: Any?): Any? = getter.call(receiver)

    override fun getDelegate(receiver: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(p1: Any?): Any? = getter.call(p1)
}

internal class KMutableProperty1Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : KProperty1Proxy(state, callInterceptor), KMutableProperty1<Any?, Any?> {
    override val setter: KMutableProperty1.Setter<Any?, Any?> =
        object : Setter(state.property.setter!!), KMutableProperty1.Setter<Any?, Any?> {
            override fun invoke(p1: Any?, p2: Any?) = call(p1, p2)

            override fun call(vararg args: Any?) {
                checkArguments(2, args.size)
                val receiverParameter = (setter.dispatchReceiverParameter ?: setter.extensionReceiverParameter)!!
                val receiver = environment.convertToState(args[0], receiverParameter.getActualType())
                val valueParameter = setter.valueParameters.single()
                val value = environment.convertToState(args[1], valueParameter.getActualType())
                callInterceptor.interceptProxy(setter, listOf(receiver, value))
            }

            override fun callBy(args: Map<KParameter, Any?>) {
                TODO("Not yet implemented")
            }
        }

    override fun set(receiver: Any?, value: Any?) = setter.call(receiver, value)
}