actual class Foo {
    actual fun foo(n: Int, s: String) {

    }
}

fun Foo.testJvm() {
    foo(1, "2")
}