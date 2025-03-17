// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75197

interface D {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Any): Int?
}

interface E {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Any) : <!UNRESOLVED_REFERENCE!>ErrorType<!>
}

typealias TA = Int

interface F {
    operator fun compareTo(other: Any): TA
}

fun test1(x: D) = x <!COMPARE_TO_TYPE_MISMATCH!><<!> 1
fun test2(x: D) = x.compareTo(2)
fun test3(y: E) = y > 3
fun test4(y: F) = y > 4
