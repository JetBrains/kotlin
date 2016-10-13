package a

fun <T, R, S> foo(block: (T)-> R, <!UNUSED_PARAMETER!>second<!>: (T)-> S) = block

fun main(args: Array<String>) {
    val fff = { <!UNUSED_PARAMETER!>x<!>: Int -> <!UNRESOLVED_REFERENCE!>aaa<!> }
    foo(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>fff<!>, { x -> x + 1 })
}

