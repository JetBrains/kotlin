// "Create function 'foo' from usage" "true"

class A<T>(val n: T) {
    fun foo(a: Int): A<T> = throw Exception()

    fun foo(arg: T, s: String): A<T> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    val a: A<Int> = A(1).foo(2, "2")
}