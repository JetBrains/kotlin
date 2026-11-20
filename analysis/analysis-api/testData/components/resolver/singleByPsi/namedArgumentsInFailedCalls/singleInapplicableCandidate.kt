// COMPILATION_ERRORS
// KT-90174
class Foo(val x: String, val y: String)

fun usage() {
    Foo(<caret>x = "hello")
}
