open class Foo {
    fun foo(a: IntArray) {}
    fun foo(a: Int, b: Int) {}
}

fun Foo.foo(i: Int) {}
fun Foo.foo() {}

fun test() {
    Foo().foo(bar())
}