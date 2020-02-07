class A {
    fun bar() {
        val foo: String.() -> Unit = {} // (1)
        fun String.foo(): Unit {} // (2)
        "1".foo() // resolves to (2)
        with("2") {
            foo() // BUG: resolves to (1) in old FE, but to (2) in FIR
        }
    }
}
class B {
    val foo: String.() -> Unit = {} // (1)
    fun String.foo(): Unit {} // (2)
    fun bar() {
        "1".foo() // resolves to (2)
        with("2") {
            foo() // resolves to (2)
        }
    }
}