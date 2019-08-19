expect class Foo {
    fun String.foo(n: Int)
}

fun Foo.test() {
    "1".foo(2)
}