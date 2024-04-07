// ISSUE: KT-64840
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<F> {
    val a: F
}

interface B<G> : A<G> {
    val b: G
}

fun <X> withCallback(x: X, c: Controller<in X>, p: (X) -> Unit) {}

fun main(a: A<String>) {
    val x = generate {
        withCallback(a, this) {
            (it as B).b.length
            it.b.length
            it.a.length
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("A<kotlin.String>")!>x<!>
}
