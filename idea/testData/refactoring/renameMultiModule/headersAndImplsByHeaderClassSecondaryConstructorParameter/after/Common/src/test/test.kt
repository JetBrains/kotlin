package test

expect class Foo {
    constructor(x: Int)
}

fun test() {
    Foo(1)
    Foo(x = 1)
}