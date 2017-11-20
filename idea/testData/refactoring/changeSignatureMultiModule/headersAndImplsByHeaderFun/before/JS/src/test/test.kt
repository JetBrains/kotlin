package test

actual fun foo() { }
actual fun foo(n: Int) { }
actual fun bar(n: Int) { }

fun test() {
    foo()
    foo(1)
    bar(1)
}