// RUN_PIPELINE_TILL: FRONTEND
package a

fun <T, R, S> foo(block: (T)-> R, second: (T)-> S) = block

fun main() {
    val fff = <!UNRESOLVED_REFERENCE!>{ x: Int -> <!UNRESOLVED_REFERENCE!>aaa<!> }<!>
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!ARGUMENT_TYPE_MISMATCH!>fff<!>, { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> x + 1 })
}
