// "Create extension function 'foo'" "true"

class A<T>(val items: List<T>) {
    fun test(): Int {
        return items.foo<T, Int, String>(2, "2")
    }
}

fun <E, T, U> List<E>.foo(t: T, u: U): T {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}
