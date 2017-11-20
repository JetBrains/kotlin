package test

actual class Foo {
    actual fun <caret>foo(n: Int) {

    }
}

fun test(f: Foo) {
    f.foo(1)
}