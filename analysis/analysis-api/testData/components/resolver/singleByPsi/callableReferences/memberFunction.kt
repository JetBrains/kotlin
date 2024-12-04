interface Foo {
    fun foo(a: Int)
}

fun test() {
    consume(<expr>Foo::foo</expr>)
}

fun consume(f: (Foo, Int) -> Unit) {}