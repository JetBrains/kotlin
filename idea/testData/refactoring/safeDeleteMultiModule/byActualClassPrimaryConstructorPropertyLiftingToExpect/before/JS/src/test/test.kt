package test

actual class Foo actual constructor(val <caret>n: Int) {
    constructor(s: String): this(0)
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo(1)
    Foo(n = 1)
}