// "Create function 'foo' from usage" "true"

class A<T>(val n: T) {
    fun foo(i: Int, s: String): A<T> = throw Exception()

    fun foo(arg: T): A<T> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    val a: A<Int> = A(1).foo(2)
}