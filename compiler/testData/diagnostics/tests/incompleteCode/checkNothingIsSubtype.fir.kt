// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION
package d

interface A<T>

fun <T> infer(a: A<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(nothing: Nothing?) {
    val i = <!CANNOT_INFER_PARAMETER_TYPE!>infer<!>(<!ARGUMENT_TYPE_MISMATCH!>nothing<!>)
}

fun sum(a : IntArray) : Int {
<!ITERATOR_MISSING!>for (n
<!SYNTAX!>return<!><!SYNTAX!><!> "?"<!>
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
