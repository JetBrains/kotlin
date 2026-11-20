// COMPILATION_ERRORS
// KT-90174
fun foo(x: String, y: String) {}
fun foo(n: Int) {}

fun usage() {
    foo(<caret>x = "hello")
}
