expect class Foo {
    fun foo(n: Int, s: String)
}

fun Foo.test() {
    foo(1, "2")
}