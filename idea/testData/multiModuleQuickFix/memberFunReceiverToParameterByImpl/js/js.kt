// "Convert receiver to parameter" "true"

actual class Foo {
    actual fun <caret>String.foo(n: Int) {

    }
}

fun Foo.test() {
    "1".foo(2)
}