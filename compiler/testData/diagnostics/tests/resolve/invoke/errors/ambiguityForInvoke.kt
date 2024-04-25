// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke(i: Int, a: Any) {}
fun Int.invoke(a: Any, i: Int) {}

fun foo(i: Int) {
    <!NONE_APPLICABLE!>i<!>(1, 1)

    <!NONE_APPLICABLE!>5<!>(1, 2)
}
