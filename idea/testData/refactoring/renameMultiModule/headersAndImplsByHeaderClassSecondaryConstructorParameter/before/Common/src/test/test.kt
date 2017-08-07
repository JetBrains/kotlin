package test

header class Foo {
    constructor(/*rename*/n: Int)
}

fun test() {
    Foo(1)
    Foo(n = 1)
}