trait Foo {
    fun foo(f: (Int) -> Unit)
}
fun foo(x: Foo) {
    <caret>x foo { it * 2 }
}
