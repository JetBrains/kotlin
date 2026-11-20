// COMPILATION_ERRORS
// KT-90174
fun foo(x: Int, y: Int = 0) {}
fun foo(x: Int, z: String = "") {}

fun usage() {
    foo(<caret>x = 1)
}
