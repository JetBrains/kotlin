@kotlin.coroutines.experimental.RestrictsSuspension
class RestrictedController

suspend fun Any?.extFun() {}
suspend fun suspendFun() {}

fun generate(<!UNUSED_PARAMETER!>c<!>: suspend RestrictedController.() -> Unit) {}

fun test() {
    generate {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extFun<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>suspendFun<!>()
    }
}
