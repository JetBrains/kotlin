class Foo {
    class Bar {
        fun bar() {}
    }
}

fun test(bar: Foo.Bar) {
    bar.<caret>bar()
}