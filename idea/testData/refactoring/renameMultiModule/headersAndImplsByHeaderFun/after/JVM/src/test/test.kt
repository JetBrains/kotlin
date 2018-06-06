package test

actual fun baz() { }
actual fun baz(n: Int) { }
actual fun bar(n: Int) { }

fun test() {
    baz()
    baz(1)
    bar(1)
}