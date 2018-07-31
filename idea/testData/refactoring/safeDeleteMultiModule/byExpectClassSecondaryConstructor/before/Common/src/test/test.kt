package test

expect open class Foo {
    <caret>constructor(n: Int)
}

fun test() = Foo(1)