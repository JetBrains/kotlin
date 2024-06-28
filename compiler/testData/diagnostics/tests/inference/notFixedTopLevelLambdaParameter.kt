// FIR_IDENTICAL
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <T1> Controller<T1>.forEach(x: (T1) -> Unit) {}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun main() {
    generate {
        forEach {
            foo(it)
        }
    }.length
}

fun foo(x: String) {}
