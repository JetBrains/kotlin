// FALSE

// Navigation from "class B: A()" should move to valid constructor even after changing type in lambda

open class A(l: String) {
    constructor(x: Int) : this("$x")
}

fun <T> foo(l: () -> T) = l()

class B: A(foo { "1"<caret> })

