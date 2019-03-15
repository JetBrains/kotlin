/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KCallables")
package kotlin.reflect.full

import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.Continuation
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.*

/**
 * Returns a parameter representing the `this` instance needed to call this callable,
 * or `null` if this callable is not a member of a class and thus doesn't take such parameter.
 */
@SinceKotlin("1.1")
val KCallable<*>.instanceParameter: KParameter?
    get() = parameters.singleOrNull { it.kind == KParameter.Kind.INSTANCE }

/**
 * Returns a parameter representing the extension receiver instance needed to call this callable,
 * or `null` if this callable is not an extension.
 */
@SinceKotlin("1.1")
val KCallable<*>.extensionReceiverParameter: KParameter?
    get() = parameters.singleOrNull { it.kind == KParameter.Kind.EXTENSION_RECEIVER }

/**
 * Returns parameters of this callable, excluding the `this` instance and the extension receiver parameter.
 */
@SinceKotlin("1.1")
val KCallable<*>.valueParameters: List<KParameter>
    get() = parameters.filter { it.kind == KParameter.Kind.VALUE }

/**
 * Returns the parameter of this callable with the given name, or `null` if there's no such parameter.
 */
@SinceKotlin("1.1")
fun KCallable<*>.findParameterByName(name: String): KParameter? {
    return parameters.singleOrNull { it.name == name }
}

/**
 * Calls a callable in the current suspend context. If the callable is not a suspend function, behaves as [KCallable.call].
 * Otherwise, calls the suspend function with current continuation.
 */
@SinceKotlin("1.3")
suspend fun <R> KCallable<R>.callSuspend(vararg args: Any?): R {
    if (!this.isSuspend) return call(*args)
    if (this !is KFunction<*>) throw IllegalArgumentException("Cannot callSuspend on a property $this: suspend properties are not supported yet")
    return suspendCoroutineUninterceptedOrReturn { call(*args, it) }
}

/**
 * Calls a callable in the current suspend context. If the callable is not a suspend function, behaves as [KCallable.callBy].
 * Otherwise, calls the suspend function with current continuation.
 */
@SinceKotlin("1.3")
suspend fun <R> KCallable<R>.callSuspendBy(args: Map<KParameter, Any?>): R {
    if (!this.isSuspend) return callBy(args)
    if (this !is KFunction<*>) throw IllegalArgumentException("Cannot callSuspendBy on a property $this: suspend properties are not supported yet")
    val kCallable = asKCallableImpl() ?: throw KotlinReflectionInternalError("This callable does not support a default call: $this")
    return suspendCoroutineUninterceptedOrReturn<R> { kCallable.callDefaultMethod(args, it) }
}