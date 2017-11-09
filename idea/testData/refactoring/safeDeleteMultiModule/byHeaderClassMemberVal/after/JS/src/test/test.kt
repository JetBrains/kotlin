package test

actual class Foo {
    val foo get() = 1
}

fun test(f: Foo) = f.foo