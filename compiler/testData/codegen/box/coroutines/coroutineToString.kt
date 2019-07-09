// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES
package test
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class A<T : String> {
    suspend fun foo() {}

    suspend fun bar(): T {
        foo()
        return suspendCoroutineUninterceptedOrReturn { x ->
            x.resume(x.toString() as T)
            COROUTINE_SUSPENDED
        }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = A<String>().bar()
    }

    return if (result == "Continuation at test.A.bar(coroutineToString.kt:16)") "OK" else "Fail: $result"
}
