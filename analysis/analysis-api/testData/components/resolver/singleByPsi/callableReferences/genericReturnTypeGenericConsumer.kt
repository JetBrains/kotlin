interface Foo<T> {
    fun foo(): T
}

fun test() {
    consume(<expr>Foo<Int>::foo</expr>)
}

fun <T> consume(f: (Foo<T>) -> T) {}