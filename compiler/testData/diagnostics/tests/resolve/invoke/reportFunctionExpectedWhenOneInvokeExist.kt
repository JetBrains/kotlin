// !DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke() {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!FUNCTION_EXPECTED, DEBUG_INFO_MISSING_UNRESOLVED!>identifier<!>()
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>identifier<!>(123)
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>identifier<!>(1, 2)
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fn<!>()
}