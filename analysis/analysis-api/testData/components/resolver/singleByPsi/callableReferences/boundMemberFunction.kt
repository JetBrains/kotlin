interface Foo {
    fun foo(a: Int)
}

fun test(obj: Foo) {
    consume(<expr>obj::foo</expr>)
}

fun consume(f: (Int) -> Unit) {}