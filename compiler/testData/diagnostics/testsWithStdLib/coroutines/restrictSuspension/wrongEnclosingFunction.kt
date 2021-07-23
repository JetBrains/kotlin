// FIR_IDENTICAL
// SKIP_TXT
@kotlin.coroutines.RestrictsSuspension
class RestrictedController {
    suspend fun yield() {}
}

fun generate(c: suspend RestrictedController.() -> Unit) {}

fun runBlocking(x: suspend () -> Unit) {}

fun test() {
    generate {
        runBlocking {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>()
        }
    }
}
