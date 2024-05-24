interface Foo

fun Foo.foo(a: Int) {}

fun test(foo: Foo) {
    consume(<expr>Foo::foo</expr>)
}

fun consume(f: (Foo, Int) -> Unit) {}