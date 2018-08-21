// !LANGUAGE: +ReleaseCoroutines
// WITH_COROUTINES
// WITH_REFLECT
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JVM_IR

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.full.callSuspendBy

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    suspend fun noArgs() = "OK"

    suspend fun twoArgs(a: String, b: String) = "$a$b"
}

suspend fun twoArgs(a: String, b: String) = "$a$b"

fun ordinary() = "OK"

suspend fun withDefault(s: String = "OK") = s

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
        val callable = A::class.members.find { it.name == "noArgs" }!!
        res = callable.callSuspendBy(mapOf(callable.parameters.first() to A())) as String?
    }
    if (res != "OK") return res ?: "FAIL 1"
    builder {
        val callable = A::class.members.find { it.name == "twoArgs" }!!
        res = callable.callSuspendBy(mapOf(callable.parameters[0] to A(), callable.parameters[1] to "O", callable.parameters[2] to "K")) as String?
    }
    if (res != "OK") return res ?: "FAIL 2"
    builder {
        res = ::twoArgs.callSuspendBy(mapOf(::twoArgs.parameters[0] to "O", ::twoArgs.parameters[1] to "K")) as String?
    }
    if (res != "OK") return res ?: "FAIL 3"
    builder {
        res = ::ordinary.callSuspendBy(emptyMap()) as String?
    }
    if (res != "OK") return res ?: "FAIL 4"
    builder {
        res = ::withDefault.callSuspendBy(emptyMap()) as String?
    }
    builder {
        ::suspending.callSuspendBy(emptyMap())
    }
    log += "suspended;"
    proceed()
    if (log != "before;suspended;after;") return log
    return res ?: "FAIL 5"
}