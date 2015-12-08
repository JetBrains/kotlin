package test

fun String.foo(n: Int, <caret>f: (Int) -> Unit) {
    println(n)
}

fun bar() {
    "".foo(10) { n -> println(n) }
}