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

@Experimental(level = Experimental.Level.ERROR)
annotation class ExperimentalTime
