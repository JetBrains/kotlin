// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: a
// ERROR: Unsupported [literal prefixes and suffixes]
// ERROR: None of the following functions can be called with the arguments supplied: <br>public constructor A(x: Int) defined in A<br>public constructor A(l: String) defined in A
// Navigation from "class B: A()" should move to valid constructor even after changing type in lambda

open class A(l: String) {
    constructor(x: Int) : this("$x")
}

fun <T> foo(l: () -> T) = l()

class B: A(foo { "1"<caret> })

