// !LANGUAGE: +ReleaseCoroutines
// WITH_COROUTINES
// WITH_REFLECT
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JVM_IR

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.full.callSuspend

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    suspend fun noArgs() = "OK"

    suspend fun twoArgs(a: String, b: String) = "$a$b"
}

suspend fun twoArgs(a: String, b: String) = "$a$b"

fun ordinary() = "OK"

var log = ""

var proceed = {}

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> { cont ->
    proceed = {
        cont.resumeWith(SuccessOrFailure.success(Unit))
    }
    COROUTINE_SUSPENDED
}

suspend fun suspending() {
    log += "before;"
    suspendHere()
    log += "after;"
}

fun box(): String {
    var res: String? = ""
    builder {
        res = A::class.members.find { it.name == "noArgs" }?.callSuspend(A()) as String?
    }
    if (res != "OK") return res ?: "FAIL 1"
    builder {
        res = A::class.members.find { it.name == "twoArgs" }?.callSuspend(A(), "O", "K") as String?
    }
    if (res != "OK") return res ?: "FAIL 2"
    builder {
        res = ::twoArgs.callSuspend("O", "K") as String?
    }
    if (res != "OK") return res ?: "FAIL 3"
    builder {
        res = ::ordinary.callSuspend() as String?
    }
    builder {
        ::suspending.callSuspend()
    }
    log += "suspended;"
    proceed()
    if (log != "before;suspended;after;") return log
    return res ?: "FAIL 4"
}