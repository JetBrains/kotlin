// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import kotlin.test.assertEquals

suspend fun ArrayList<Int>.yield(v: Int): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    this.add(v)
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

tailrec suspend fun ArrayList<Int>.fromTo(from: Int, to: Int) {
    if (from > to) return
    yield(from)
    return fromTo(from + 1, to)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    val result = arrayListOf<Int>()

    builder {
        result.fromTo(1, 5)
    }

    assertEquals(listOf(1, 2, 3, 4, 5), result)

    return "OK"
}

