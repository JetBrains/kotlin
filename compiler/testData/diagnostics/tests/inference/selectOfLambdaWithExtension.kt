// FIR_IDENTICAL
// SKIP_TXT

typealias A = CharSequence.(Int) -> Unit

var w: Int = 1

fun myPrint(x: Int) {}

fun <T> select(vararg x: T) = x[0]

val a1: A = select(
    { a: Int -> myPrint(a + this.length + 1) },
    { a: Int -> myPrint(a + this.length + 2) }
)

val a2 = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>select<!>(
    { a: Int -> myPrint(a + this.length + 1) },
    fun CharSequence.(a: Int) { myPrint(a + this.length + 2) },
    { a: Int -> myPrint(a + this.length + 3) }
)
