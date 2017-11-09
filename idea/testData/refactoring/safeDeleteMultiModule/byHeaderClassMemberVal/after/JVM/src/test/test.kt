package test

actual class Foo {
    val foo get() = 2
}

fun test(f: Foo) = f.foo