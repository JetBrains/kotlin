package test

actual class C {
    actual fun foo(x: Int) {

    }
}

fun test(c: C) {
    c.foo(1)
    c.foo(x = 1)
}