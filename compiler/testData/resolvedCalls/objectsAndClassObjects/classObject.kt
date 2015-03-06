class Foo {
    default object {
        val baz = Foo()
    }
}

fun test() {
    Foo.<caret>baz
}


