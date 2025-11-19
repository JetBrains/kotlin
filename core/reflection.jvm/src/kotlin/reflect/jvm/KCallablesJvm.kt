/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("KCallablesJvm")

package kotlin.reflect.jvm

import java.lang.reflect.AccessibleObject
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.asReflectCallable
import kotlin.reflect.jvm.internal.callDefaultMethod
import kotlin.reflect.jvm.internal.calls.InlineClassCallContext
import kotlin.reflect.jvm.internal.calls.InlineClassCallMode

/**
 * Provides a way to suppress JVM access checks for a callable.
 *
 * @getter returns `true` if JVM access checks are suppressed for this callable object.
 *         For a property, that means that all its accessors (getter, and setter for `var` properties) are accessible.
 *
 * @setter if set to `true`, suppresses JVM access checks for this callable object.
 *         For a property, both accessors are made accessible.
 *
 * @see [java.lang.reflect.AccessibleObject]
 */
var KCallable<*>.isAccessible: Boolean
    get() {
        return when (this) {
            is KMutableProperty ->
                javaField?.isAccessible ?: true &&
                        javaGetter?.isAccessible ?: true &&
                        javaSetter?.isAccessible ?: true
            is KProperty ->
                javaField?.isAccessible ?: true &&
                        javaGetter?.isAccessible ?: true
            is KProperty.Getter ->
                property.javaField?.isAccessible ?: true &&
                        javaMethod?.isAccessible ?: true
            is KMutableProperty.Setter<*> ->
                property.javaField?.isAccessible ?: true &&
                        javaMethod?.isAccessible ?: true
            is KFunction ->
                javaMethod?.isAccessible ?: true &&
                        (this.asReflectCallable()?.defaultCaller?.member as? AccessibleObject)?.isAccessible ?: true &&
                        this.javaConstructor?.isAccessible ?: true
            else -> throw UnsupportedOperationException("Unknown callable: $this ($javaClass)")
        }
    }


    set(value) {
        when (this) {
            is KMutableProperty -> {
                javaField?.isAccessible = value
                javaGetter?.isAccessible = value
                javaSetter?.isAccessible = value
            }
            is KProperty -> {
                javaField?.isAccessible = value
                javaGetter?.isAccessible = value
            }
            is KProperty.Getter -> {
                property.javaField?.isAccessible = value
                javaMethod?.isAccessible = value
            }
            is KMutableProperty.Setter<*> -> {
                property.javaField?.isAccessible = value
                javaMethod?.isAccessible = value
            }
            is KFunction -> {
                javaMethod?.isAccessible = value
                (this.asReflectCallable()?.defaultCaller?.member as? AccessibleObject)?.isAccessible = true
                this.javaConstructor?.isAccessible = value
            }
            else -> throw UnsupportedOperationException("Unknown callable: $this ($javaClass)")
        }
    }


/**
 * JVM-specific variant of KCallable.callBy that accepts unboxed arguments for value classes and returns an unboxed value for value-class return types.
 * This is intended for Java interop frameworks that already work with underlying JVM representations of Kotlin value classes.
 *
 * If the callable has no value classes in parameters or return type, this behaves the same as callBy.
 */
@SinceKotlin("2.1")
fun <R> KCallable<R>.callByUnboxed(args: Map<KParameter, Any?>): Any? {
    val kCallable = asReflectCallable() ?: throw KotlinReflectionInternalError("This callable does not support a default call: $this")
    return InlineClassCallContext.with(InlineClassCallMode.FULLY_UNBOXED) {
        if (kCallable.isAnnotationConstructor) kCallable.callAnnotationConstructor(args) else kCallable.callDefaultMethod(args, null)
    }
}

/**
 * JVM-specific variant of KCallable.callSuspendBy that accepts unboxed arguments for value classes and returns an unboxed value for value-class return types.
 * This is intended for Java interop frameworks that already work with underlying JVM representations of Kotlin value classes.
 *
 * If the callable has no value classes in parameters or return type, this behaves the same as callSuspendBy.
 */
@SinceKotlin("2.1")
suspend fun <R> KCallable<R>.callSuspendByUnboxed(args: Map<KParameter, Any?>): Any? {
    if (!this.isSuspend) return callByUnboxed(args)
    if (this !is KFunction<*>) throw IllegalArgumentException("Cannot callSuspendByUnboxed on a property $this: suspend properties are not supported yet")
    val kCallable = asReflectCallable() ?: throw KotlinReflectionInternalError("This callable does not support a default call: $this")
    return InlineClassCallContext.with(InlineClassCallMode.FULLY_UNBOXED) {
        suspendCoroutineUninterceptedOrReturn { kCallable.callDefaultMethod(args, it) }
    }
}
