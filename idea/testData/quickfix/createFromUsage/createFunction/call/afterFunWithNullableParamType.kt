// "Create function 'foo' from usage" "true"

class A<T>(val n: T) {
    fun foo(arg: T?): A<Int> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    val a: A<Int> = A(true).foo(false as Boolean?)
}