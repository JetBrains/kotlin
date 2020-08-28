// COMMON_COROUTINES_TEST
// SKIP_TXT
@COROUTINES_PACKAGE.RestrictsSuspension
class RestrictedController {
    suspend fun yield() {}
}

fun generate(c: suspend RestrictedController.() -> Unit) {}

fun runBlocking(x: suspend () -> Unit) {}

fun test() {
    generate {
        runBlocking {
            yield()
        }
    }
}
