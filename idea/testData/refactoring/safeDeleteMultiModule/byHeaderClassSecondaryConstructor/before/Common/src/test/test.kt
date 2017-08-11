package test

header open class Foo {
    <caret>constructor(n: Int)
}

fun test() = Foo(1)