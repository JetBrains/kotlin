package test

actual class Foo(s: String) {
    constructor(n: Int): this("") {
        val x = n + 1
    }
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo(1)
    Foo(n = 1)
}