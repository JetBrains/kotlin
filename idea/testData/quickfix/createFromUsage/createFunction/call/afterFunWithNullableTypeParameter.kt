// "Create function 'foo' from usage" "true"

class A<T>(val n: T) {
    fun foo(arg: T): A<String> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    val a: A<String> = A(1 as Int?).foo(2)
}