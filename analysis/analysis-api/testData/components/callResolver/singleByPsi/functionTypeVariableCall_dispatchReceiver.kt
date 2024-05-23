// IGNORE_STABILITY_K1: candidates
class A {
    val f: String.() -> Unit = {}
    fun test() {
        "".<expr>f()</expr>
    }
}