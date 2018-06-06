package test

actual class Foo(s: String) {
    constructor(): this("") {
        val x = n + 1
    }
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo()
    Foo()
}