class Foo {
    val foo: String
        get() = "foo"
}

fun test(foo: Foo) {
    foo.<caret>foo
}