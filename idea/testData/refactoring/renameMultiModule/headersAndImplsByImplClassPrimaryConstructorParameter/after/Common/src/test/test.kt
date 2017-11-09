package test

expect class Foo(x: Int)

fun test() {
    Foo(1)
    Foo(x = 1)
}