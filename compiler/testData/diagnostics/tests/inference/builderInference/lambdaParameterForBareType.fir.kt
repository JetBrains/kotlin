// ISSUE: KT-64840 (K2/PCLA difference)
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
        predicate(a, this) { it is <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!> }
    }.a
}
