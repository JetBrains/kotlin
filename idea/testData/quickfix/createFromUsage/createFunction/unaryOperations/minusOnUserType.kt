// "Create member function 'A.unaryMinus'" "true"

class A<T>(val n: T)

fun test() {
    val a = <caret>-A(1)
}