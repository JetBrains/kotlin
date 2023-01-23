open class Base
class DoesNotImplementBase

fun <T, V> exampleGenericFunction(func: V) where T: Base, V: (T) -> Unit {

}

fun main() {
    val func: (DoesNotImplementBase) -> Unit = { }
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR!>exampleGenericFunction<!>(func) // expected this to be a compilation error as the T: Base constraint should not be satisfied
}
