// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend Context.() -> Unit) {
    c.startCoroutine(Context(), EmptyContinuation)
}

class Foo {
    fun foo() {
        val fionaClient = Any()
        coVerify { fionaClient was Called }
    }
}

object Called

var res = "FAIL"

class Context {
    infix fun Any.was(called: Called) {
        res = "OK"
    }
}

fun coVerify(verifyBlock: suspend Context.() -> Unit) {
    builder(verifyBlock)
}

fun box(): String {
    Foo().foo()
    return res
}