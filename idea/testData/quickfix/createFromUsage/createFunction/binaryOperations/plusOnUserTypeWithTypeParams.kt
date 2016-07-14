// "Create member function 'A.plus'" "true"

class A<T>(val n: T)

fun <U> test(u: U) {
    val a: A<U> = A(u) <caret>+ 2
}