// "Create function 'foo' from usage" "true"

class A<T>(val n: T)

fun test<U>(u: U) {
    val a: A<U> = A(u).<caret>foo(u)
}