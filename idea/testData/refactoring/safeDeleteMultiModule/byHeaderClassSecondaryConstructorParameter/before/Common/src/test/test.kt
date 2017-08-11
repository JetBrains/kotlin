package test

header class Foo {
    constructor(<caret>n: Int)
}

fun test() {
    Foo(1)
    Foo(n = 1)
}