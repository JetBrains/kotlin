package test

actual class Foo {
    actual val foo get() = 2
}

fun test(f: Foo) = f.foo