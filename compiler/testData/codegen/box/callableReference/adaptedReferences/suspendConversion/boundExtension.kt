// !LANGUAGE: +SuspendConversion
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class C {
    var test = "failed"
}

fun C.foo() {
    test = "OK"
}

fun box(): String {
    val c = C()
    runSuspend(c::foo)
    return c.test
}