// COMPILATION_ERRORS
// KT-90174
open class Base(val x: String, val y: String) {
    constructor(n: Int) : this(n.toString(), n.toString())
}

class Derived : Base(<caret>x = "hello")
