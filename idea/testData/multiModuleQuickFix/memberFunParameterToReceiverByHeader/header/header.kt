// "Convert parameter to receiver" "true"

header class Foo {
    fun foo(n: Int, <caret>s: String)
}

fun Foo.test() {
    foo(1, "2")
}