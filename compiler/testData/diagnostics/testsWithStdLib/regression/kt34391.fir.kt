// !LANGUAGE: +NewInference

fun main() {
    val list = listOf(A())
    list.forEach(A::<!OPT_IN_USAGE_ERROR!>foo<!>)
    list.forEach {
        it.<!OPT_IN_USAGE_ERROR!>foo<!>()
    }
}

class A {
    @ExperimentalTime
    fun foo() {
        println("a")
    }
}

@<!OPT_IN_IS_NOT_ENABLED!>RequiresOptIn<!>(level = RequiresOptIn.Level.ERROR)
annotation class ExperimentalTime
