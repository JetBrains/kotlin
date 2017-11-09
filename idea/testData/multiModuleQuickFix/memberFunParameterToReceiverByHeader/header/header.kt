// "Convert parameter to receiver" "true"

expect class Foo {
    fun foo(n: Int, <caret>s: String)
}

fun Foo.test() {
    foo(1, "2")
}