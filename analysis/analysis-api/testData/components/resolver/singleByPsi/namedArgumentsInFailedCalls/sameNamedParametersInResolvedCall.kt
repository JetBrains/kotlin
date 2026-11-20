// COMPILATION_ERRORS
// KT-90174
fun foo(x: Int = 0, x: String) {}

fun usage() {
    foo(<caret>x = "hello")
}
