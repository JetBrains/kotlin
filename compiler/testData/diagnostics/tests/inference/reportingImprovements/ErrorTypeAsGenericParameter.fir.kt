package a

fun <T, R, S> foo(block: (T)-> R, second: (T)-> S) = block

fun main() {
    val fff = { x: Int -> <!UNRESOLVED_REFERENCE!>aaa<!> }
    foo(<!ARGUMENT_TYPE_MISMATCH!>fff<!>, { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> x + 1 })
}
