fun foo(x: Foo) {
    x.<caret>foo { it * 2 }
}

interface Foo {
    fun foo(f: (Int) -> Int)
}
