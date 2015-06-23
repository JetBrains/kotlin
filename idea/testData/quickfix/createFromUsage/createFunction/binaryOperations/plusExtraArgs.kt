// "Create member function 'plus'" "true"

class A<T>(val n: T) {
    fun plus(): A<T> = throw Exception()
}

fun test() {
    val a: A<Int> = A(1) + <caret>2
}