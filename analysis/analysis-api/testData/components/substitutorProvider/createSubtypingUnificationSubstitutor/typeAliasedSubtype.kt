class Foo
typealias FooAlias = Foo

fun usage(xx: FooAlias, yy: Foo) {
    x<caret_1_left>x
    y<caret_1_right>y
}
