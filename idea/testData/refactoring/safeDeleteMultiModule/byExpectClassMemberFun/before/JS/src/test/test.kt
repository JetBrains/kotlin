package test

actual class Foo {
    actual fun foo(n: Int) {

    }
}

fun test(f: Foo) {
    f.foo(1)
}