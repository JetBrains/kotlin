package test

expect class Foo {
    fun foo(<caret>n: Int)
}

fun test(f: Foo) {
    f.foo(1)
    f.foo(n = 1)
}