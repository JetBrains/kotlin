// "Create member function 'foo'" "true"

class A<T>(val n: T)

fun test() {
    val a: A<Int>? = A(1).<caret>foo(2)
}