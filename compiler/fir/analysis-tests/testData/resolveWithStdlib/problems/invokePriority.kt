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

class E {
    object f {
        operator fun invoke() = Unit // (1)
    }
    companion object {
        val f: () -> Unit = {} // (2)
    }
}

fun main() {
    E.<!AMBIGUITY!>f<!>() // Resolves to (2) in old FE (ambiguity in FIR)
    E.f.invoke() // Resolves to (1)
}
