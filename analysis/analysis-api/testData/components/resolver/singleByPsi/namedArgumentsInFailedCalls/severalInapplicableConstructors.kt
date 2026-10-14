// COMPILATION_ERRORS
// KT-90174
class Foo(val x: String, val y: String) {
    constructor(base: Foo) : this(base.x, base.y)
}

fun usage() {
    Foo(<caret>x = "hello")
}
