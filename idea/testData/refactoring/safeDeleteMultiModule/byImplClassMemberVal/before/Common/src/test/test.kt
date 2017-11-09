package test

expect class Foo {
    val foo: Int
}

fun test(f: Foo) = f.foo