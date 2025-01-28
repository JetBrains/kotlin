// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-64840 (K2/PCLA difference)
// LATEST_LV_DIFFERENCE
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<F> {
    val a: F?
}

interface B<G> : A<G>

fun <X> predicate(x: X, c: Controller<in X>, p: (X) -> Boolean) {}

fun main(a: A<*>) {
    generate {
        predicate(a, this) { it is B }
    }.a
}
