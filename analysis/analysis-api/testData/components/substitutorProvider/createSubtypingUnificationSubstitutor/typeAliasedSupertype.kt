class Foo
typealias FooAlias = Foo

fun usage(xx: Foo, yy: FooAlias) {
    x<caret_1_base>x
    y<caret_1_target>y
}
