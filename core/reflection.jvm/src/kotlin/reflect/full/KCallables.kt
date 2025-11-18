/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KCallables")

package kotlin.reflect.full

import org.jetbrains.kotlin.load.java.JvmAbi
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.asReflectCallable
import kotlin.reflect.jvm.internal.callDefaultMethod
import kotlin.reflect.jvm.internal.calls.toInlineClass

/**
 * Returns a parameter representing the `this` instance needed to call this callable,
 * or `null` if this callable is not a member of a class and thus doesn't take such parameter.
 */
@SinceKotlin("1.1")
val KCallable<*>.instanceParameter: KParameter?
    get() = parameters.singleOrNull { it.kind == KParameter.Kind.INSTANCE }

/**
 * Returns context parameters of this callable.
 */
@ExperimentalContextParameters
val KCallable<*>.contextParameters: List<KParameter>
    get() = parameters.filter { it.kind == KParameter.Kind.CONTEXT }

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
    val result = suspendCoroutineUninterceptedOrReturn<R> { call(*args, it) }
    // If suspend function returns Unit and tail-call, it might appear, that it returns not Unit,
    // see comment above replaceReturnsUnitMarkersWithPushingUnitOnStack for explanation.
    // In this case, return Unit manually.
    @Suppress("UNCHECKED_CAST")
    if (returnType.classifier == Unit::class && !returnType.isMarkedNullable) return (Unit as R)
    return result
}

/**
 * Calls a callable in the current suspend context. If the callable is not a suspend function, behaves as [KCallable.callBy].
 * Otherwise, calls the suspend function with current continuation.
 */
@SinceKotlin("1.3")
suspend fun <R> KCallable<R>.callSuspendBy(args: Map<KParameter, Any?>): R {
    if (!this.isSuspend) return callBy(args)
    if (this !is KFunction<*>) throw IllegalArgumentException("Cannot callSuspendBy on a property $this: suspend properties are not supported yet")
    val kCallable = asReflectCallable() ?: throw KotlinReflectionInternalError("This callable does not support a default call: $this")
    val result = suspendCoroutineUninterceptedOrReturn<R> { kCallable.callDefaultMethod(args, it) }
    // If suspend function returns Unit and tail-call, it might appear, that it returns not Unit,
    // see comment above replaceReturnsUnitMarkersWithPushingUnitOnStack for explanation.
    // In this case, return Unit manually.
    @Suppress("UNCHECKED_CAST")
    if (returnType.classifier == Unit::class && !returnType.isMarkedNullable) return (Unit as R)
    return result
}

/**
 * Calls this callable with a mapping of parameters to arguments where value-class parameters may be provided unboxed,
 * and returns the result with value-class return value unboxed as well.
 * If a parameter is not found in the mapping and is not optional (as per [KParameter.isOptional]),
 * or its type does not match the type of the provided value (after boxing), an exception is thrown.
 */
@SinceKotlin("2.3")
fun <R> KCallable<R>.callByUnboxed(args: Map<KParameter, Any?>): R {
    val boxedArgs: Map<KParameter, Any?> = boxValueClassArgsIfNeeded(this, args)
    val result = callBy(boxedArgs)
    @Suppress("UNCHECKED_CAST")
    return unboxValueClassReturnIfNeeded(this, result) as R
}

/**
 * Calls this callable in the current suspend context with a mapping of parameters to arguments where value-class
 * parameters may be provided unboxed, and returns the result with value-class return value unboxed as well.
 * If the callable is not a suspend function, behaves as [callByUnboxed].
 */
@SinceKotlin("2.3")
suspend fun <R> KCallable<R>.callSuspendByUnboxed(args: Map<KParameter, Any?>): R {
    if (!this.isSuspend) return callByUnboxed(args)
    if (this !is KFunction<*>) throw IllegalArgumentException("Cannot callSuspendByUnboxed on a property $this: suspend properties are not supported yet")
    val boxedArgs: Map<KParameter, Any?> = boxValueClassArgsIfNeeded(this, args)
    val result = callSuspendBy(boxedArgs)
    @Suppress("UNCHECKED_CAST")
    return unboxValueClassReturnIfNeeded(this, result) as R
}

// --- Helpers ---

private fun boxValueClassArgsIfNeeded(callable: KCallable<*>, args: Map<KParameter, Any?>): Map<KParameter, Any?> {
    if (args.isEmpty()) return args
    val out = HashMap<KParameter, Any?>(args.size)
    for ((param, value) in args) {
        out[param] = boxValueClassParamIfNeeded(param.type, value)
    }
    return out
}

private fun boxValueClassParamIfNeeded(paramType: KType, value: Any?): Any? {
    val inlineClass = paramType.toInlineClass() ?: return value
    if (value == null) return null
    if (inlineClass.isInstance(value)) return value

    val unboxMethod = try {
        inlineClass.getDeclaredMethod("unbox" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS)
    } catch (_: NoSuchMethodException) {
        null
    }
    val boxMethod = try {
        if (unboxMethod != null) inlineClass.getDeclaredMethod(
            "box" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS,
            unboxMethod.returnType
        ) else null
    } catch (_: NoSuchMethodException) {
        null
    }

    if (boxMethod != null) {
        if (!boxMethod.canAccess(null)) boxMethod.isAccessible = true
        return boxMethod.invoke(null, value)
    }
    return value
}

private fun unboxValueClassReturnIfNeeded(callable: KCallable<*>, result: Any?): Any? {
    val inlineClass = callable.returnType.toInlineClass() ?: return result
    if (result == null) return null
    val unboxMethod = try {
        inlineClass.getDeclaredMethod("unbox" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS)
    } catch (_: NoSuchMethodException) {
        null
    }
    if (unboxMethod != null) {
        if (!unboxMethod.canAccess(result)) unboxMethod.isAccessible = true
        return unboxMethod.invoke(result)
    }
    return result
}
