//KT-657 Semantic checks for when without condition
package kt657

fun foo() =
    when {
        cond1() -> 12
        cond2() -> 2
        <!TYPE_MISMATCH_IN_CONDITION!>4<!> -> 34
        <!TYPE_MISMATCH_IN_CONDITION!>#(1, 2)<!> -> 3
        <!EXPECTED_CONDITION!>in 1..10<!> -> 34
        <!TYPE_MISMATCH_IN_CONDITION!>4<!> -> 38
        <!EXPECTED_CONDITION!>is val a in 1..10<!> -> 23
        <!EXPECTED_CONDITION!>is Int<!> -> 33
        <!EXPECTED_CONDITION!>is <!TYPE_MISMATCH_IN_TUPLE_PATTERN!>#(val a, 3)<!><!> -> 2
        <!EXPECTED_CONDITION!>!is <!TYPE_MISMATCH_IN_TUPLE_PATTERN!>#(*, 1100)<!><!> -> 3
        <!EXPECTED_CONDITION!>is *<!> -> 34
    }

fun cond1() = false

fun cond2() = true
