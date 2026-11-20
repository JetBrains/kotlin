// COMPILATION_ERRORS
// KT-90174
fun foo(x: String, y: String) {}
fun foo(x: Int, z: Int) {}

fun usage() {
    foo(<caret>x = "hello")
}
