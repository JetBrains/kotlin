// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.intrinsics.*
import COROUTINES_PACKAGE.*

class A {
    var isMinusAssignCalled = false
    operator suspend fun minusAssign(y: String): Unit = suspendCoroutineUninterceptedOrReturn { x ->
        if (y != "56") return@suspendCoroutineUninterceptedOrReturn Unit
        isMinusAssignCalled = true
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
}

val a = A()

suspend fun foo5() {
    a -= "56"
    if (!a.isMinusAssignCalled) throw RuntimeException("fail 6")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder{ foo5() }
    return "OK"
}
