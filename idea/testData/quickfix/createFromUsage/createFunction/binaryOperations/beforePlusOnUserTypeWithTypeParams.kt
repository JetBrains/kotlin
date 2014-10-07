// "Create function 'plus' from usage" "true"

class A<T>(val n: T)

fun test<U>(u: U) {
    val a: A<U> = A(u) <caret>+ 2
}