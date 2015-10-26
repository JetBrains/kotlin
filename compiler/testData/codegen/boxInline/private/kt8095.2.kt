package test

class C(private val a : String) {
    internal inline fun g(x: (s: String) -> Unit) {
        x(a)
    }
}