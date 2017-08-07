package test

impl class Foo(s: String) {
    constructor(/*rename*/n: Int): this("")
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo(1)
    Foo(n = 1)
}