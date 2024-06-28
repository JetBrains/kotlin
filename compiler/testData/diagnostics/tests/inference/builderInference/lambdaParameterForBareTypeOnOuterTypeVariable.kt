// ISSUE: KT-64840
class Controller<T> {
    val t: T get() = TODO()
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<F> {
    val a: F?
}

interface B<G> : A<G>

fun main(a: A<*>) {
    generate {
        yield(a)
        <!USELESS_IS_CHECK!>t is <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!><!>
    }.a
}
