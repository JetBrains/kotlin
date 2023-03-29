// FIR_IDENTICAL
interface Base

class DoesNotImplementBase

fun <T, V> exampleGenericFunction(func: V) where T: Base, V: (T) -> Unit {

}

fun main() {
    val func: (DoesNotImplementBase) -> Unit = { }
    <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>exampleGenericFunction<!>(func) // expected this to be a compilation error as the T: Base constraint should not be satisfied
}