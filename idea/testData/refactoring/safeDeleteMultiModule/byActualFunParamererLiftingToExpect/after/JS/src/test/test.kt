package test

actual fun foo() {
    n + 1
}

fun test() {
    foo()
    foo()
}