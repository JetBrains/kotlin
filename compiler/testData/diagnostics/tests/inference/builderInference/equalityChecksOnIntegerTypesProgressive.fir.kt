// !LANGUAGE: +ProperEqualityChecksInBuilderInferenceCalls
// WITH_STDLIB

fun main(x: Long, y: Int) {
    sequence {
        <!EQUALITY_NOT_APPLICABLE_WARNING!>1L == 3<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>x == 3<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>3 == 1L<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>3 == x<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>y == x<!>

        <!EQUALITY_NOT_APPLICABLE_WARNING!>1L === 3<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>x === 3<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>3 === 1L<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>3 === x<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING!>y === x<!>

        yield("")
    }
}