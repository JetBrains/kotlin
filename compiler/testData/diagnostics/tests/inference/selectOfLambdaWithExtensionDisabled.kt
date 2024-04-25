// LANGUAGE: -ConsiderExtensionReceiverFromConstrainsInLambda
// SKIP_TXT

typealias A = CharSequence.(Int) -> Unit

var w: Int = 1

fun myPrint(x: Int) {}

fun <T> select(vararg x: T) = x[0]

val a1: A = select(
    { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>a: Int<!> -> myPrint(a + this.length + 1) },
    { a: Int -> myPrint(a + this.length + 2) }
)

val a2 = <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION("TypeVariable(_RP1); CharSequence, Int; final class and interface")!>select<!>(
    { a: Int -> myPrint(a + this.<!UNRESOLVED_REFERENCE!>length<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1) },
    fun CharSequence.(a: Int) { myPrint(a + this.length + 2) },
    { a: Int -> myPrint(a + this.<!UNRESOLVED_REFERENCE!>length<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 3) }
)
