//KT-657 Semantic checks for when without condition
package kt657

class Pair<A, B>(a: A, b: B)

fun foo() =
    when {
        cond1() -> 12
        cond2() -> 2
        <!CONDITION_TYPE_MISMATCH!>4<!> -> 34
        <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>Pair(1, 2)<!> -> 3
        <!ARGUMENT_TYPE_MISMATCH, EXPECTED_CONDITION!>in 1..10<!> -> 34
        <!CONDITION_TYPE_MISMATCH!>4<!> -> 38
        <!EXPECTED_CONDITION!>is Int<!> -> 33
        else -> 34
    }

fun cond1() = false

fun cond2() = true
