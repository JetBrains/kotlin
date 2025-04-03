// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-76527

fun foo(f: context(String) Int.() -> Unit) {
    with("") {
        f(1)
    }
}