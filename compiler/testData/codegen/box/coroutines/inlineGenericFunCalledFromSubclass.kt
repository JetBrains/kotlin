// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// WITH_REFLECT
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend fun foo(x: suspend () -> String): String = x()

abstract class A {
    inline suspend fun <reified T : Any> baz(): String {
        return foo {
            suspendThere(T::class.simpleName!!)
        }
    }

}
class B : A() {
    suspend fun bar(): String {
        return baz<OK>()
    }
}
fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}
class OK
fun box(): String {
    var result = "fail"
    builder {
        result = B().bar()
    }

    return result
}
