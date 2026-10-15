// LANGUAGE: +ContextParameters +ExplicitContextArguments
// COMPILATION_ERRORS
// KT-90174
context(s: String)
fun foo(x: Int) {}

fun foo(s: String, y: Int) {}

fun usage() {
    foo(<caret>s = "")
}
