/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB
// WITH_COROUTINES

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

val sb = StringBuilder()

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun s1(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    sb.appendLine("s1")
    x.resume(42)
    COROUTINE_SUSPENDED
}

suspend fun s2(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    sb.appendLine("s2")
    x.resumeWithException(Error("Error"))
    COROUTINE_SUSPENDED
}

suspend fun s3(value: Int): Int = suspendCoroutineUninterceptedOrReturn { x ->
    sb.appendLine("s3")
    x.resume(value)
    COROUTINE_SUSPENDED
}

fun f1(): Int {
    sb.appendLine("f1")
    return 117
}

fun f2(): Int {
    sb.appendLine("f2")
    return 1
}

fun f3(x: Int, y: Int): Int {
    sb.appendLine("f3")
    return x + y
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0

    builder {
        while (result < 3)
            result = s3(result) + 1
    }

    sb.appendLine(result)

    assertEquals("""
        s3
        s3
        s3
        3

    """.trimIndent(), sb.toString())
    return "OK"
}