// LANGUAGE: +ContextParameters +ExplicitContextArguments
// COMPILATION_ERRORS
// KT-90174
context(x: String)
fun foo(x: Int) {}

fun foo(x: String, y: Int) {}

fun usage() {
    foo(<caret>x = "")
}
