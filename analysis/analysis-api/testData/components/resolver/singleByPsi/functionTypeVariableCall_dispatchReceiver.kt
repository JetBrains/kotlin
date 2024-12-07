class A {
    val f: String.() -> Unit = {}
    fun test() {
        "".<expr>f()</expr>
    }
}