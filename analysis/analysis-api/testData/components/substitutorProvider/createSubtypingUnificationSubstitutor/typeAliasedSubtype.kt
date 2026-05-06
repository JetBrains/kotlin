class Foo
typealias FooAlias = Foo

fun usage(xx: FooAlias, yy: Foo) {
    x<caret_1_base>x
    y<caret_1_target>y
}
