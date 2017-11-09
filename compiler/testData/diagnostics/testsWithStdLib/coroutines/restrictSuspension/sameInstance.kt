// !DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE

@kotlin.coroutines.experimental.RestrictsSuspension
class RestrictedController {
    suspend fun member() {}
}

suspend fun RestrictedController.extension() {}

fun generate(f: suspend RestrictedController.() -> Unit) {}

fun test() {
    generate() l@ {
        member()
        extension()

        this.member()
        this.extension()

        val foo = this
        foo.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        foo.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extension<!>()

        this@l.member()
        this@l.extension()

        with(1) {
            this@l.member()
            this@l.extension()
        }
    }
}

suspend fun RestrictedController.l() {
    member()
    extension()

    this.member()
    this.extension()

    val foo = this
    foo.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
    foo.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extension<!>()

    this@l.member()
    this@l.extension()

    with(1) {
        this@l.member()
        this@l.extension()
    }

}
