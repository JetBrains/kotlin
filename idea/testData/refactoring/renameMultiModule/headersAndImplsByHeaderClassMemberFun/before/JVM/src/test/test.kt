package test

actual class C {
    actual fun foo() { }
    actual fun foo(n: Int) { }
    actual fun bar(n: Int) { }
}

fun test(c: C) {
    c.foo()
    c.foo(1)
    c.bar(1)
}