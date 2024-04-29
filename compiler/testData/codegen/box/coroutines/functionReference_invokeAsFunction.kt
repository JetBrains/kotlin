/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-66098: ClassCastException
// IGNORE_BACKEND: WASM
// WITH_STDLIB
// WITH_COROUTINES

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

class Foo(val x: Int) {
    suspend fun bar(y: Int) = foo(y) + x
}

suspend fun foo(x: Int) = x

fun box(): String {
    val ref = Foo(42)::bar
    assertEquals(159, (ref as Function2<Int, Continuation<Int>, Any?>)(117, EmptyContinuation))
    return "OK"
}
