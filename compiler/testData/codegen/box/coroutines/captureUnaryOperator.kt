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
        coVerify { !fionaClient }
    }
}

var res = "FAIL"

class Context {
    operator fun Any.not() {
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