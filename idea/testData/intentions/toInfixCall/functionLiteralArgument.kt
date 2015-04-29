fun foo(x: Foo) {
    x.<caret>foo { it * 2 }
}

trait Foo {
    fun foo(f: (Int) -> Int)
}
