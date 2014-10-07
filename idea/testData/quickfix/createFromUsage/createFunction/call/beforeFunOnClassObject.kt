// "Create function 'foo' from usage" "true"

class A<T>(val n: T) {
    class object {

    }
}

fun test() {
    val a: Int = A.<caret>foo(2)
}