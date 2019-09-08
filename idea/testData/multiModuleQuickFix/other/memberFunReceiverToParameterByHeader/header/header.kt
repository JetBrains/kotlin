// "Convert receiver to parameter" "true"

expect class Foo {
    fun <caret>String.foo(n: Int)
}

fun Foo.test() {
    "1".foo(2)
}