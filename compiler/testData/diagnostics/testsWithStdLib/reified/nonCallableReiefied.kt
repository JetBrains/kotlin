class A<T>(val x: Array<T>) {
    val y: Int = x[0].toString().length

    fun foo(a: T) {
        x[0] = a
    }

    fun <R> bar(a: Array<R>): Int = a[0].toString().length
}

fun <T> baz(a: Array<T>): String = a[0].toString()
