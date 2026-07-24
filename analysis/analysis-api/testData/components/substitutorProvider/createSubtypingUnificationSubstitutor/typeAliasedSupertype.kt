class Foo
typealias FooAlias = Foo

fun usage(xx: Foo, yy: FooAlias) {
    x<caret_1_left>x
    y<caret_1_right>y
}
