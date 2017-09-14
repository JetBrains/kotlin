package test

impl open class Foo {
    impl <caret>constructor(n: Int)
}

fun test() = Foo(1)