// DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke(a: Int) {}
fun Int.invoke(a: Int, b: Int) {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!NONE_APPLICABLE!>identifier<!>()
    <!NONE_APPLICABLE!>identifier<!>(123)
    <!NONE_APPLICABLE!>identifier<!>(1, 2)
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fn<!>()
}
