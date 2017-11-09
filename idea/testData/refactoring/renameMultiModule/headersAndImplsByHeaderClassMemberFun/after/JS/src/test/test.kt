package test

actual class C {
    actual fun baz() { }
    actual fun baz(n: Int) { }
    actual fun bar(n: Int) { }
}

fun test(c: C) {
    c.baz()
    c.baz(1)
    c.bar(1)
}