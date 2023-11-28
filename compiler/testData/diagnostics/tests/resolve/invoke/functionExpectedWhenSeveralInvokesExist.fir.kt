// !DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke(a: Int) {}
fun Int.invoke(a: Int, b: Int) {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!NONE_APPLICABLE!>identifier<!>()
    <!NONE_APPLICABLE!>identifier<!>(123)
    <!OPERATOR_MODIFIER_REQUIRED!>identifier<!>(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!TOO_MANY_ARGUMENTS!>2<!>)
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fn<!>()
}
