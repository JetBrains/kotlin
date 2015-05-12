interface Foo {
    fun inc() : Foo
    fun not() : Foo
}
fun foo(x: Foo) {
    !(<caret>x.inc())
}