actual class Foo {
    actual fun String.foo(n: Int) {

    }
}

fun Foo.test() {
    "1".foo(2)
}