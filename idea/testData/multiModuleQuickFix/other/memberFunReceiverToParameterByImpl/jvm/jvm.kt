actual class Foo {
    actual fun String.foo(n: Int) {

    }
}

fun Foo.test1() {
    "1".foo(2)
}