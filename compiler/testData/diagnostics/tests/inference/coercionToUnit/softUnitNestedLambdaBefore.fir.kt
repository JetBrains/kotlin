// LANGUAGE: -ExpectedUnitAsSoftConstraint
interface Inv<F>

fun unitRun(x: () -> Unit) {}

fun <R> foo(x: () -> Inv<R>): R = TODO()

fun main(x: Inv<Int>) {
    unitRun {
        foo { <!ARGUMENT_TYPE_MISMATCH!>x<!> }
    }
}

