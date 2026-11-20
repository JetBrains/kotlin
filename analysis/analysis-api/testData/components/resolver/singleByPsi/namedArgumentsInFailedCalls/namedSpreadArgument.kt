// COMPILATION_ERRORS
// KT-90174
fun foo(vararg x: Int, y: String) {}
fun foo(vararg x: Int, z: String) {}

fun usage() {
    foo(<caret>x = *intArrayOf(1))
}
