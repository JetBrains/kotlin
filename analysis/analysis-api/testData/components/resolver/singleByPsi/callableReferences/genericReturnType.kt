interface Foo<T> {
    fun foo(): T
}

fun test() {
    consume(<expr>Foo<Int>::foo</expr>)
}

fun consume(f: (Foo<Int>) -> Int) {}