package test

class A() {
    private val x = "OK"
    internal inline fun foo(p: (String) -> Unit) {
        p(x)
    }
}