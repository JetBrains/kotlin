// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// CHECK_BYTECODE_LISTING
import helpers.*
import COROUTINES_PACKAGE.*

sealed class X {
    class A : X()
    class B : X()
}

var log = ""

suspend fun process(a: X.A) { log = "${log}from A;" }
suspend fun process(b: X.B) { log = "${log}from B;" }

suspend fun process(x: X) = when (x) {
    is X.A -> process(x)
    is X.B -> process(x)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        process(X.A())
        process(X.B())
    }
    if (log != "from A;from B;") return log
    return "OK"
}