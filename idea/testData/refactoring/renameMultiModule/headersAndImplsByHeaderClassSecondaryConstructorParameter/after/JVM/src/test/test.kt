package test

actual class Foo(s: String) {
    constructor(x: Int): this("")
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo(1)
    Foo(x = 1)
}