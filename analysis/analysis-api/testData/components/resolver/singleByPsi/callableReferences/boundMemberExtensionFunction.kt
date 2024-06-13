interface Foo {
    fun Bar.foo(a: Int)
}

interface Bar

fun test(foo: Foo) {
    with(foo) {
        // 'foo' is a member and an extension at the same time.
        // References to such elements are prohibited.
        consume(<expr>Bar::foo</expr>)
    }
}

fun consume(f: (Bar, Int) -> Unit) {}
// COMPILATION_ERRORS