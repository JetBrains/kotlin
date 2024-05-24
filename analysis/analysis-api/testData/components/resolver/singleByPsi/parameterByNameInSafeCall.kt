class Foo {
    fun bar(name: Int) {}
}

fun usage(foo: Foo) {
    foo?.bar(na<caret>me = 10)
}
