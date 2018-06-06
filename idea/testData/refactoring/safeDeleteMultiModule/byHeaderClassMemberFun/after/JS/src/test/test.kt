package test

actual class Foo {
    fun foo(n: Int) {

    }
}

fun test(f: Foo) {
    f.foo(1)
}