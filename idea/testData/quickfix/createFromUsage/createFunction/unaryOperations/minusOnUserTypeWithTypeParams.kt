// "Create member function 'unaryMinus'" "true"

class A<T>(val n: T)

fun <U> test(u: U) {
    val a: A<U> = <caret>-A(u)
}