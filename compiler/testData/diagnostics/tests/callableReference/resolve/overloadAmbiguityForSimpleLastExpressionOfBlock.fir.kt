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
            <!AMBIGUITY!>foo<!>(C)
        } else {
            bar(C)
        }

val l1: Int
    get() = when {
        true -> <!AMBIGUITY!>foo<!>(C)
        false -> { <!AMBIGUITY!>foo<!>(C) }
        else -> bar(C)
    }
