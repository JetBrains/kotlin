// "Create function 'foo' from usage" "true"

class A<T>(val n: T)

fun test() {
    val a: A<String> = A(1 as Int?).<caret>foo(2)
}