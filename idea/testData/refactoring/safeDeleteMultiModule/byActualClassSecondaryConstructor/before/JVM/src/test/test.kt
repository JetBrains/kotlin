package test

actual open class Foo {
    actual constructor(n: Int)
}

fun test() = Foo(2)