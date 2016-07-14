// "Create member function 'A.Companion.foo'" "true"

class A<T>(val n: T) {
    companion object {

    }
}

fun test() {
    val a: Int = A.<caret>foo(2)
}