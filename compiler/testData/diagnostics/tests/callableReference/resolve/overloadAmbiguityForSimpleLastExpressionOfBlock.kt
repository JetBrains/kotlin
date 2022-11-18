// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A
interface B
object C : A, B

fun foo(a: A): Int = 0
fun foo(b: B): Double = 0.0

fun bar(a: A): Int = 0

val l0: Int
    get() =
        if (1 < 2) {
            <!ARGUMENT_TYPE_MISMATCH!><!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(C)<!>
        } else {
            bar(C)
        }

val l1: Int
    get() = when {
        true -> <!ARGUMENT_TYPE_MISMATCH!><!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(C)<!>
        false -> { <!ARGUMENT_TYPE_MISMATCH!><!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(C)<!> }
        else -> bar(C)
    }
