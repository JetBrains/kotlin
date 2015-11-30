class Foo {
    companion object {
        val baz = Foo()
    }
}

fun test() {
    Foo.<caret>baz
}
