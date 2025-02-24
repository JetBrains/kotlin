// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-75124
private class Context

context(Context)
fun foo() {
}

fun main() {
    Context().run {
        foo()
    }
}