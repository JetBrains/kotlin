// "Create member function 'unaryMinus'" "true"

class A<T>(val n: T)

fun test() {
    val a = <caret>-A(1)
}