class Foo
typealias FooAlias = Foo

fun FooAlias.foo<caret>Ext() {}

fun usage(x: Foo) {
    <expr>x</expr>
}