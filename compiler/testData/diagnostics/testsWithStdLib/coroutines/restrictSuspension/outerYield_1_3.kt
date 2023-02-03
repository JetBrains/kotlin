// FIR_IDENTICAL
// !LANGUAGE: +ExperimentalBuilderInference
// !OPT_IN: kotlin.RequiresOptIn
// SKIP_TXT

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

@kotlin.coroutines.RestrictsSuspension
class RestrictedController<T> {
    suspend fun yield(x: T) {}

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

fun <T> buildSequence(c: suspend RestrictedController<T>.() -> Unit) {}

suspend fun <T> RestrictedController<T>.yield2(x: T) {}

fun test() {
    buildSequence<Int> a@{
        buildSequence<Int> b@{
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

    buildSequence<Int> {
        buildSequence<String> {
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

    buildSequence<Int> a@{
        yield(1)
        yield2(1)
        buildSequence {
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

    buildSequence<String> {
        yield("")
        RestrictedController<String>().<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>yield<!>("1")
    }
}
