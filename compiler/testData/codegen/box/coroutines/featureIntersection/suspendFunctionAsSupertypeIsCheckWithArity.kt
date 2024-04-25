// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS, JVM, JVM_IR
// FIR status: not supported in JVM
// LANGUAGE: +SuspendFunctionAsSupertype

import kotlin.coroutines.*

class C: suspend () -> Unit {
    override suspend fun invoke() {
    }
}

interface I: suspend () -> Unit {}

fun interface FI: suspend () -> Unit {}

interface I2: suspend (Int) -> Unit, suspend (Int, Int) -> Unit {

}

@Suppress("INCOMPATIBLE_TYPES")
fun box(): String {
    val c = C()
    if (c !is SuspendFunction0<*>) return "FAIL 1"
    if (c is SuspendFunction1<*, *>) return "FAIL 2"

    val i = object : I {
        override suspend fun invoke() {
        }
    }
    if (i !is SuspendFunction0<Unit>) return "FAIL 3"
    if (i is SuspendFunction1<*, *>) return "FAIL 4"

    val fi = object : FI {
        override suspend fun invoke() {
        }
    }
    if (fi !is SuspendFunction0<Unit>) return "FAIL 5"
    if (fi is SuspendFunction1<*, *>) return "FAIL 6"

    val o = object : suspend () -> Unit {
        override suspend fun invoke() {
        }
    }
    if (o !is SuspendFunction0<Unit>) return "FAIL 7"
    if (o is SuspendFunction1<*, *>) return "FAIL 8"

    val i2 = object : I2 {
        override suspend fun invoke(i1: Int) {
        }

        override suspend fun invoke(i1: Int, i2: Int) {
        }
    }
    if (i2 !is SuspendFunction1<*, *>) return "FAIL 9"
    if (i2 !is SuspendFunction2<*, *, *>) return "FAIL 10"
    if (i2 is SuspendFunction3<*, *, *, *>) return "FAIL 11"

    return "OK"
}