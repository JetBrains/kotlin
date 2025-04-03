// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-76527

fun foo(f: context(String) Int.() -> Unit) {
    f("", 1)
    with("") {
        f(1)
        1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f<!>()
    }
}