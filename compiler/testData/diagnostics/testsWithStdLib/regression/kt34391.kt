// !LANGUAGE: +NewInference

fun main() {
    val list = listOf(A())
    list.forEach(A::<!EXPERIMENTAL_API_USAGE_ERROR!>foo<!>)
    list.forEach {
        it.<!EXPERIMENTAL_API_USAGE_ERROR!>foo<!>()
    }
}

class A {
    @ExperimentalTime
    fun foo() {
        println("a")
    }
}

@<!EXPERIMENTAL_IS_NOT_ENABLED!>Experimental<!>(level = <!EXPERIMENTAL_IS_NOT_ENABLED!>Experimental<!>.Level.ERROR)
annotation class ExperimentalTime
