package test

actual class Foo(x: Int) {
    constructor(s: String): this(0)
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo(1)
    Foo(x = 1)
}