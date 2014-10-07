// "Create function 'foo' from usage" "true"

class A<T>(val n: T)

fun test() {
    val a: A<Int> = A(true).<caret>foo(false as Boolean?)
}