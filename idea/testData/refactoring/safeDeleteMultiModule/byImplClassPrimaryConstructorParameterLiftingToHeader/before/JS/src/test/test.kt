package test

impl class Foo(<caret>n: Int) {
    val x = n + 1
    constructor(s: String): this(0)
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo(1)
    Foo(n = 1)
}