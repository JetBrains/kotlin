package a

fun <T, R, S> foo(block: (T)-> R, second: (T)-> S) = block

fun main() {
    val fff = { x: Int -> <!UNRESOLVED_REFERENCE!>aaa<!> }
    <!INAPPLICABLE_CANDIDATE!>foo<!>(fff, { x -> x + 1 })
}

