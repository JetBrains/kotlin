// IGNORE_STABILITY_K1
class A {
    val f: String.() -> Unit = {}
    fun test() {
        "".<expr>f()</expr>
    }
}