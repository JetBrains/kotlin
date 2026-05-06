class Foo
typealias FooAlias1 = Foo
typealias FooAlias2 = Foo


fun usage(xx: FooAlias2, yy: FooAlias1) {
    x<caret_1_base>x
    y<caret_1_target>y
}
