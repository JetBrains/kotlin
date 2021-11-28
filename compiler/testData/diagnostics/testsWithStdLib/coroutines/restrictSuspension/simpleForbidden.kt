// FIR_IDENTICAL
@kotlin.coroutines.RestrictsSuspension
class RestrictedController

suspend fun Any?.extFun() {}
suspend fun suspendFun() {}

fun generate(c: suspend RestrictedController.() -> Unit) {}

fun test() {
    generate {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extFun<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>suspendFun<!>()
    }
}
