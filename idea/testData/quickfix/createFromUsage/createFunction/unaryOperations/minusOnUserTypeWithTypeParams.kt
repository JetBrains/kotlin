// "Create member function 'minus'" "true"

class A<T>(val n: T)

fun test<U>(u: U) {
    val a: A<U> = <caret>-A(u)
}