//KT-657 Semantic checks for when without condition
package kt657

class Pair<A, B>(<!UNUSED_PARAMETER!>a<!>: A, <!UNUSED_PARAMETER!>b<!>: B)

fun foo() =
    when {
        cond1() -> 12
        cond2() -> 2
        <!TYPE_MISMATCH_IN_CONDITION!>4<!> -> 34
        <!TYPE_MISMATCH_IN_CONDITION!>Pair(1, 2)<!> -> 3
        <!EXPECTED_CONDITION!>in 1..10<!> -> 34
        <!TYPE_MISMATCH_IN_CONDITION!>4<!> -> 38
        <!EXPECTED_CONDITION!>is Int<!> -> 33
        else -> 34
    }

fun cond1() = false

fun cond2() = true
