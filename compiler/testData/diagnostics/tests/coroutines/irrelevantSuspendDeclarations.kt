// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
class Controller {
    suspend fun suspendHere(a: String) = 1
}

class A {
    suspend fun suspendHere(a: Int) = 1
}

fun builder(c: suspend Controller.() -> Unit) {}

fun test() {
    builder {
        suspendHere("")

        with(A()) {
            suspendHere("")
            // With the new convention calling a suspension member with receiver different from the one obtained from the coroutine is OK
            suspendHere(1)
        }
    }
}
