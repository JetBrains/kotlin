// WITH_COROUTINES
// WITH_REFLECT
// TARGET_BACKEND: JVM

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

suspend fun withManyDefault(
    s0: String = "NOT OK 0",
    s1: String = "NOT OK 1",
    s2: String = "NOT OK 2",
    s3: String = "NOT OK 3",
    s4: String = "NOT OK 4",
    s5: String = "NOT OK 5",
    s6: String = "NOT OK 6",
    s7: String = "NOT OK 7",
    s8: String = "NOT OK 8",
    s9: String = "NOT OK 9",
    s10: String = "NOT OK 10",
    s11: String = "NOT OK 11",
    s12: String = "NOT OK 12",
    s13: String = "NOT OK 13",
    s14: String = "NOT OK 14",
    s15: String = "NOT OK 15",
    s16: String = "NOT OK 16",
    s17: String = "NOT OK 17",
    s18: String = "NOT OK 18",
    s19: String = "NOT OK 19",
    s20: String = "NOT OK 20",
    s21: String = "NOT OK 21",
    s22: String = "NOT OK 22",
    s23: String = "NOT OK 23",
    s24: String = "NOT OK 24",
    s25: String = "NOT OK 25",
    s26: String = "NOT OK 26",
    s27: String = "NOT OK 27",
    s28: String = "NOT OK 28",
    s29: String = "NOT OK 29",
    s30: String = "NOT OK 30",
    s31: String = "NOT OK 31",
    s32: String = "NOT OK 32",
    ok: String = "OK"
) = ok

var log = ""

var proceed = {}

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> { cont ->
    proceed = {
        cont.resumeWith(Result.success(Unit))
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
    if (res != "OK") return res ?: "FAIL 5"
    builder {
        res = ::withManyDefault.callSuspendBy(emptyMap()) as String?
    }
    builder {
        ::suspending.callSuspendBy(emptyMap())
    }
    log += "suspended;"
    proceed()
    if (log != "before;suspended;after;") return log
    return res ?: "FAIL 5"
}
