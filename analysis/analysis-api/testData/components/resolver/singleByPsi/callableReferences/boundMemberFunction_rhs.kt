interface Foo {
    fun foo(a: Int)
}

fun test(obj: Foo) {
    consume(obj::<expr>foo</expr>)
}

fun consume(f: (Int) -> Unit) {}