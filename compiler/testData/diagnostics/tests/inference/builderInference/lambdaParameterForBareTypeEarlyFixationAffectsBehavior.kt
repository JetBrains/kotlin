// !CHECK_TYPE
// ISSUE: KT-64840

class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface CommonSupertype
interface A<F> : CommonSupertype
interface B<G> : A<G>
interface C : CommonSupertype

fun <X> predicate(x: X, c: Controller<X>, p: (X) -> Unit) {}

fun main(a: A<*>, c: C) {
    val x1 = generate {
        predicate(a, this) { x ->
            // x is B
        }

        yield(c)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("CommonSupertype")!>x1<!>

    val x2 = generate {
        predicate(a, this) { x ->
            <!USELESS_IS_CHECK!>x is <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!><!>
        }

        yield(c)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("CommonSupertype")!>x2<!>
}
