package test

expect class Foo(<caret>n: Int)

fun test() {
    Foo(1)
    Foo(n = 1)
}