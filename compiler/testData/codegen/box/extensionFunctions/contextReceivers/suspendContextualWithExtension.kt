// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_COROUTINES
// WITH_STDLIB
// FIR status: context receivers aren't yet supported

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Context {
    fun c() = "O"
}

class Extension {
    fun e() = "K"
}

context(Context)
suspend fun Extension.suspendingTest() = c() + e()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail"

    builder {
        with(Extension()) {
            with(Context()) {
                result = suspendingTest()
            }
        }
    }

    return result
}