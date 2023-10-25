// FIR_IDENTICAL
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface Base
interface Derived : Base

fun foo(
    base: Base,
    derived: Derived,
) {
    val t = generate {
        twoBooleans(yield(base), yield(derived))
    }

    // Should be Base, not just Derived
    <!DEBUG_INFO_EXPRESSION_TYPE("Base")!>t<!>
}

fun twoBooleans(x: Boolean, y: Boolean) {}