package test

actual class C {
    actual fun foo() { }
    actual fun baz(n: Int) { }
    actual fun bar(n: Int) { }
}

fun test(c: C) {
    c.foo()
    c.baz(1)
    c.bar(1)
}