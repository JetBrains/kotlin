// COMPILATION_ERRORS
// KT-90174
fun foo(x: Int = 0, x: String) {}
fun foo(x: String, y: Int = 0) {}

fun usage() {
    foo(<caret>x = "hello")
}
