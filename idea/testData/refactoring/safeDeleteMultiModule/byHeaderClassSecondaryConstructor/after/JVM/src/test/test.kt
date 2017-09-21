package test

actual open class Foo {
    constructor(n: Int)
}

fun test() = Foo(2)