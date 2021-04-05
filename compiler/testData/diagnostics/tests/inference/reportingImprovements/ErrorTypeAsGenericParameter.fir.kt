package a

fun <T, R, S> foo(block: (T)-> R, second: (T)-> S) = block

fun main() {
    val fff = { x: Int -> <!UNRESOLVED_REFERENCE!>aaa<!> }
    foo(<!ARGUMENT_TYPE_MISMATCH!>fff<!>, { x -> x <!UNRESOLVED_REFERENCE!>+<!> 1 })
}

