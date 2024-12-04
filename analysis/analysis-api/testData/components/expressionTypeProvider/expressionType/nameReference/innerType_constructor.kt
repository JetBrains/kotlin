class Foo {
    inner class Bar
}

fun foo(foo: Foo) {
    foo.<expr>Bar</expr>()
}