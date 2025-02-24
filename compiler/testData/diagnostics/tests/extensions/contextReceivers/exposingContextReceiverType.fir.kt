// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-75124
private class Context

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(<!EXPOSED_RECEIVER_TYPE!>Context<!>)
fun foo() {
}

fun main() {
    Context().run {
        foo()
    }
}