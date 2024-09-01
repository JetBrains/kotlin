// FIR_IDENTICAL
// ISSUE: KT-68889

class MyFoo {
    fun foo(): String = ""
}

class Inv<E>

fun <E1> Inv<out E1>.foo(): E1 = TODO()

class Controller<F> {
    fun yield(f: F) {}
}

fun <T> generate(x: Controller<T>.() -> Unit): T = TODO()

fun main(s: MyFoo?) {
    val z = generate {
        val x = (myLet { s } ?: return@generate).foo()
        yield(x)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
}

fun <R1> myLet(block: () -> R1): R1 = TODO()
