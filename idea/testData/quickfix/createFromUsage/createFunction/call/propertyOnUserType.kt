// "Create member function 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Replace with infix function call
// ERROR: Unresolved reference: x

class A<T>(val n: T) {
    fun foo(p: Int) {

    }
}

fun test() {
    A(1).<caret>foo(x)
}