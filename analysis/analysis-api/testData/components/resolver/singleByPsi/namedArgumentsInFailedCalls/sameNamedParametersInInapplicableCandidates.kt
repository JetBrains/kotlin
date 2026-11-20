// COMPILATION_ERRORS
// KT-90174
fun foo(x: Int, z: Int) {}
fun foo(x: Int, z: Int, x: String) {}

fun usage() {
    foo(<caret>x = "hello")
}
