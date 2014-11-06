// "Create function 'foo' from usage" "true"

class A<T>(val items: List<T>) {
    fun test(): Int {
        return items.foo<T, Int>(2, "2")
    }
}

fun <E, T> List<E>.foo(arg: T, s: String): T {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}