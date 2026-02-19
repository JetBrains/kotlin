class Foo
typealias FooAlias1 = Foo
typealias FooAlias2 = Foo

fun FooAlias1.foo<caret>Ext() {}

fun usage(x: FooAlias2) {
    <expr>x</expr>
}