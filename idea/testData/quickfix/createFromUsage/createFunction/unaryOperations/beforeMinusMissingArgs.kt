// "Create function 'minus' from usage" "true"

class A<T>(val n: T) {
    fun minus(n: Int): A<T> = throw Exception()
}

fun test() {
    val a: A<Int> = <caret>-A(1)
}