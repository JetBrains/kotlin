class Foo {
    fun bar(y: String) {}
}

fun x(foo: Foo?) {
    foo?.bar(<caret>arg)
}

