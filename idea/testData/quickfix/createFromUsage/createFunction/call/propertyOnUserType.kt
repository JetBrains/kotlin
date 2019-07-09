// "Create member function 'foo'" "false"
// ACTION: Add names to call arguments
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Unresolved reference: x

class A<T>(val n: T) {
    fun foo(p: Int) {

    }
}

fun test() {
    A(1).<caret>foo(x)
}