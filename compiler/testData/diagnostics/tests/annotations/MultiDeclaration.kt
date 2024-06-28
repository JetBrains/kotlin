// FIR_IDENTICAL
annotation class Ann

data class Pair(val x: Int, val y: Int)

fun foo(): Int {
    <!WRONG_ANNOTATION_TARGET!>@Ann<!> val (a, b) = Pair(12, 34)
    @<!UNRESOLVED_REFERENCE!>Err<!> val (c, d) = Pair(56, 78)
    return a + b + c + d
}