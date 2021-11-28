// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// !LANGUAGE: +SuspendFunctionAsSupertype

import kotlin.coroutines.*

class C: suspend () -> Unit {
    override suspend fun invoke() {
    }
}

interface I: suspend () -> Unit {}

fun interface FI: suspend () -> Unit {}

@Suppress("INCOMPATIBLE_TYPES")
fun box(): String {
    val c = C()
    if (c !is SuspendFunction0<*>) return "FAIL 1"
    if (c !is Function1<*, *>) return "FAIL 2"
    if (c is SuspendFunction1<*, *>) return "FAIL 3"

    val i = object : I {
        override suspend fun invoke() {
        }
    }
    if (i !is SuspendFunction0<Unit>) return "FAIL 4"
    if (i !is Function1<*, *>) return "FAIL 5"
    if (i is SuspendFunction1<*, *>) return "FAIL 6"

    val fi = object : FI {
        override suspend fun invoke() {
        }
    }
    if (fi !is SuspendFunction0<Unit>) return "FAIL 7"
    if (fi !is Function1<*, *>) return "FAIL 8"
    if (fi is SuspendFunction1<*, *>) return "FAIL 9"

    val o = object : suspend () -> Unit {
        override suspend fun invoke() {
        }
    }
    if (o !is SuspendFunction0<Unit>) return "FAIL 10"
    if (o !is Function1<*, *>) return "FAIL 11"
    if (o is SuspendFunction1<*, *>) return "FAIL 12"

    return "OK"
}