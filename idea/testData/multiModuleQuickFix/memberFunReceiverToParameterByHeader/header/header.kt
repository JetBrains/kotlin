// "Convert receiver to parameter" "true"

header class Foo {
    fun <caret>String.foo(n: Int)
}

fun Foo.test() {
    "1".foo(2)
}