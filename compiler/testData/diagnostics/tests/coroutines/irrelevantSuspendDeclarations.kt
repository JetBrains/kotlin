// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
class Controller {
    suspend fun suspendHere(a: String, x: Continuation<Int>) {
    }
}

class A {
    suspend fun suspendHere(a: Int, x: Continuation<Int>) {
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {}

fun test() {
    builder {
        suspendHere("")

        with(A()) {
            suspendHere("")
            // This test checks that suspending functions
            // that are not from coroutine controller can't be called by suspending convention
            suspendHere(1<!NO_VALUE_FOR_PARAMETER!>)<!>
        }
    }
}
