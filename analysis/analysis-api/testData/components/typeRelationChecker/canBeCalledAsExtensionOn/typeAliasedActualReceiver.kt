class Foo
typealias FooAlias = Foo

fun Foo.foo<caret>Ext() {}

fun usage(x: FooAlias) {
    <expr>x</expr>
}