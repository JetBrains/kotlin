// COMMON_COROUTINES_TEST
// SKIP_TXT
@COROUTINES_PACKAGE.RestrictsSuspension
class RestrictedController<T> {
    suspend fun yield(<!UNUSED_PARAMETER!>x<!>: T) {}

    suspend fun anotherYield(x: T) {
        yield(x)
        this.yield(x)

        yield2(x)
        this.yield2(x)

        with(this) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>(x)
            this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>(x)

            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>(x)
            this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>(x)
        }
    }
}

fun <T> defineSequence(<!UNUSED_PARAMETER!>c<!>: suspend RestrictedController<T>.() -> Unit) {}
suspend fun <T> RestrictedController<T>.yield2(<!UNUSED_PARAMETER!>x<!>: T) {}

fun test() {
    defineSequence<Int> a@{
        defineSequence<Int> b@{
            yield(1)
            yield2(1)
            this@b.yield(1)
            this@b.yield2(1)

            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>(2) // Should be error
            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>(2) // Should be error

            with(this) {
                <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>(3)
                this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>(3)

                <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>(3)
                this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>(3)
            }
        }
    }

    defineSequence<Int> {
        defineSequence<String> {
            yield("a")
            yield2("a")
            this.yield("b")
            this.yield2("b")

            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>(1) // Should be error
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>(1) // Should be error

            with(this) {
                <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>("")
                this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>("")

                <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>("")
                this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>("")
            }
        }
    }

    defineSequence<Int> a@{
        yield(1)
        yield2(1)
        defineSequence {
            yield("")
            yield2("")
            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>(1)
            this@a.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>(1)

            with(this) {
                <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>("")
                this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>("")

                <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>("")
                this@with.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield2<!>("")
            }
        }
    }

    defineSequence<String> {
        yield("")
        RestrictedController<String>().<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>("1")
    }
}
