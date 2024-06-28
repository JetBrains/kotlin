// FIR_IDENTICAL
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun <R> foo(w: R, x: (R) -> R): R = TODO()

fun main() {
    generate {
        yield(1)
        yield(foo(2) { 3 })
    }
}