package test

actual open class Foo {
    actual <caret>constructor(n: Int)
}

fun test() = Foo(1)