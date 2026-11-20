// LANGUAGE: +ContextParameters
// COMPILATION_ERRORS
// KT-90174
context(x: String)
fun foo(x: Int) {}

fun foo(x: Boolean) {}

fun usage() {
    foo(<caret>x = "")
}
