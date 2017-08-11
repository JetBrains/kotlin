package test

header class Foo {
    fun <caret>foo(n: Int)
}

fun test(f: Foo) {
    f.foo(1)
}