interface Foo {
    fun foo(): Int
}

interface Bar {
    fun bar(): Int
}

context(foo: Foo, _: Bar)
fun test() {
    <expr>foo</expr>
}