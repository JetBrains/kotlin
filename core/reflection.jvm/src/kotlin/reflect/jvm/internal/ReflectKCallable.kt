/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import kotlin.coroutines.Continuation
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.internal.calls.Caller

/**
 * This interface and its subinterfaces are used to provide operations supported by all implementations of [KCallable] in kotlin-reflect
 * (K1 descriptor-based, kotlin-metadata-jvm, and Java-based) that are not yet exposed in the public API of [KCallable].
 */
internal interface ReflectKCallable<out R> : KCallable<R> {
    val container: KDeclarationContainerImpl

    val rawBoundReceiver: Any?

    val receiverParameters: List<KParameter>

    // The instance which is used to perform a positional call, i.e. `call`
    val caller: Caller<*>

    // The instance which is used to perform a call "by name", i.e. `callBy`
    val defaultCaller: Caller<*>?

    fun callDefaultMethod(args: Map<KParameter, Any?>, continuationArgument: Continuation<*>?): R
}

internal interface ReflectKFunction : ReflectKCallable<Any?>, KFunction<Any?> {
    val signature: String
}

internal interface ReflectKProperty<out V> : ReflectKCallable<V>, KProperty<V> {
    val signature: String

    val javaField: Field?
}

internal interface ReflectKParameter : KParameter {
    val callable: ReflectKCallable<*>
}

internal val ReflectKCallable<*>.isBound: Boolean
    get() = rawBoundReceiver !== CallableReference.NO_RECEIVER
