// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun <T> suspendHere(x: T): T = suspendCoroutineUninterceptedOrReturn {
    it.resume(x)
    COROUTINE_SUSPENDED
}

inline class I(val x: Any?)

open class C {
    companion object {
        @JvmStatic
        protected suspend fun f(): I = I(suspendHere("OK"))
    }
}

class D : C() {
    companion object {
        suspend fun g() = f()
    }
}

fun box(): String {
    var result = "FAIL"
    builder {
        result = D.g().x as String
    }
    return result
}
