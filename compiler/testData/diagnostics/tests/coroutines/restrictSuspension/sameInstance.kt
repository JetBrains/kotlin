// !DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE

@kotlin.coroutines.RestrictSuspension
class RestrictedController {
    suspend fun member() {}
}

suspend fun RestrictedController.extension() {}

fun generate(f: suspend RestrictedController.() -> Unit) {}

fun test() {
    generate() l@ {
        member()
        extension()

        // todo
        this.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        this.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extension<!>()

        val foo = this
        foo.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        foo.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extension<!>()

        // todo
        this@l.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        this@l.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extension<!>()

        with(1) {
            // todo
            this@l.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
            this@l.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extension<!>()
        }
    }
}