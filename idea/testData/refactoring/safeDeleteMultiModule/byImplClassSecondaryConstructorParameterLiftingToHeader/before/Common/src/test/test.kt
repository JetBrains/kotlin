package test

expect class Foo(n: Int)

fun test() {
    Foo(1)
    Foo(n = 1)
}