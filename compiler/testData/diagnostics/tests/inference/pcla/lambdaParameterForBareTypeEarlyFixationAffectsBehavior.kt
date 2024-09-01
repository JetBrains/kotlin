// CHECK_TYPE
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
    // Without having `is` check
    // This PCLA/BI call works in the same way both in K1 and K2
    val x1 = generate {
        predicate(a, this) { x ->
            // x is B
        }

        yield(c)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("CommonSupertype")!>x1<!>

    // But introducing `is` on the expression of `Xv` type as LHS and a bare type on RHS
    // Leads to an early fixation of Xv to the current result type (A<*>) and automatically it leads to Sv fixation, too
    // This case works differently in K1 (BI) and in K2 (PCLA), but in both cases it's red code
    // Without the last `yield` call, it would be even green in K2
    val x2 = generate {
        predicate(a, this) { x ->
            <!USELESS_IS_CHECK!>x is <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!><!>
        }

        // For Sv we've got an EQUALITY constraint to A<*>
        // Thus not allowing C type here
        yield(c)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("CommonSupertype")!>x2<!>
}
