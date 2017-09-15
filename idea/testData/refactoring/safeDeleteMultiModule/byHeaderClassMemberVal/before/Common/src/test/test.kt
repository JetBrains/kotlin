package test

expect class Foo {
    val <caret>foo: Int
}

fun test(f: Foo) = f.foo