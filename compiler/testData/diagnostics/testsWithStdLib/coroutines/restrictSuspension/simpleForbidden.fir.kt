// COMMON_COROUTINES_TEST
@COROUTINES_PACKAGE.RestrictsSuspension
class RestrictedController

suspend fun Any?.extFun() {}
suspend fun suspendFun() {}

fun generate(c: suspend RestrictedController.() -> Unit) {}

fun test() {
    generate {
        extFun()
        suspendFun()
    }
}
