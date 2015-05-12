interface Foo {
    fun get(x : Any) : Foo
}
fun foo(x: Foo) {
    <caret>(x[x])[x]
}