package test

actual class Foo {
    actual fun foo(<caret>n: Int) {
        n + 1
    }
}

fun test(f: Foo) {
    f.foo(1)
    f.foo(n = 1)
}