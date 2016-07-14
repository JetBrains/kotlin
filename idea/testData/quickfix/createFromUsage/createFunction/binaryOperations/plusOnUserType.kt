// "Create member function 'A.plus'" "true"

class A<T>(val n: T)

fun test() {
    val a: A<Int> = A(1) <caret>+ 2
}