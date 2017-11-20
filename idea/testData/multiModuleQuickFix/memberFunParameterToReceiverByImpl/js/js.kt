// "Convert parameter to receiver" "true"

actual class Foo {
    actual fun foo(n: Int, <caret>s: String) {

    }
}

fun Foo.test() {
    foo(1, "2")
}