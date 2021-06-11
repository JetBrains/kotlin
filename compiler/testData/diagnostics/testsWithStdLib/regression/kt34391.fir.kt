// !LANGUAGE: +NewInference

fun main() {
    val list = listOf(A())
    list.forEach(A::foo)
    list.forEach {
        it.foo()
    }
}

class A {
    @ExperimentalTime
    fun foo() {
        println("a")
    }
}

@<!EXPERIMENTAL_IS_NOT_ENABLED!>RequiresOptIn<!>(level = RequiresOptIn.Level.ERROR)
annotation class ExperimentalTime
