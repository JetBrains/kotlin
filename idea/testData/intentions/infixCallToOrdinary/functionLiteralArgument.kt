interface Foo {
    fun foo(f: (Int) -> Unit)
}
fun foo(x: Foo) {
    x <caret>foo { it * 2 }
}
