// "Create function 'foo' from usage" "true"

class A<T>(val n: T)

fun test() {
    val a: A<Int> = 2.foo(A(1))
}

fun Int.foo(A: A<Int>): A<Int> {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}