// IGNORE_STABILITY_K1
class A {
    fun test() {
        "".<expr>f()</expr>
    }
}

val A.f: String.() -> Unit = {}
