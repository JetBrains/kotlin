// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke() {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!INAPPLICABLE_CANDIDATE!>identifier<!>()
    <!INAPPLICABLE_CANDIDATE!>identifier<!>(123)
    <!INAPPLICABLE_CANDIDATE!>identifier<!>(1, 2)
    1.<!INAPPLICABLE_CANDIDATE!>fn<!>()
}