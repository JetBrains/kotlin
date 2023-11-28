// !DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke() {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>identifier<!>()
    <!OPERATOR_MODIFIER_REQUIRED!>identifier<!>(<!TOO_MANY_ARGUMENTS!>123<!>)
    <!OPERATOR_MODIFIER_REQUIRED!>identifier<!>(<!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>2<!>)
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fn<!>()
}
