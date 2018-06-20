// COMMON_COROUTINES_TEST
// SKIP_TXT
@COROUTINES_PACKAGE.RestrictsSuspension
class RestrictedController {
    suspend fun yield() {}
}

fun generate(<!UNUSED_PARAMETER!>c<!>: suspend RestrictedController.() -> Unit) {}

fun runBlocking(<!UNUSED_PARAMETER!>x<!>: suspend () -> Unit) {}

fun test() {
    generate {
        runBlocking {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>()
        }
    }
}
