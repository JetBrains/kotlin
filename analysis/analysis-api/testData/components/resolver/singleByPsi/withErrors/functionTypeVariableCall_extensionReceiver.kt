// IGNORE_STABILITY_K1: candidates
class A {
    fun test() {
        "".<expr>f()</expr>
    }
}

val A.f: String.() -> Unit = {}
