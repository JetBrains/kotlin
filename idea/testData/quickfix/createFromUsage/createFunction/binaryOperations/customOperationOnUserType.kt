// "Create member function 'foo'" "true"
// ERROR: infix modifier is required on 'foo' in 'A'

class A<T>(val n: T)

fun test() {
    val a: A<Int> = A(1) <caret>foo 2
}