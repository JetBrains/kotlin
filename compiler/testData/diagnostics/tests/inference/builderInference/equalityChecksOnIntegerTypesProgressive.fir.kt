// !LANGUAGE: +ProperEqualityChecksInBuilderInferenceCalls
// WITH_STDLIB

fun main(x: Long, y: Int) {
    sequence {
        <!EQUALITY_NOT_APPLICABLE!>1L == 3<!>
        <!EQUALITY_NOT_APPLICABLE!>x == 3<!>
        <!EQUALITY_NOT_APPLICABLE!>3 == 1L<!>
        <!EQUALITY_NOT_APPLICABLE!>3 == x<!>
        <!EQUALITY_NOT_APPLICABLE!>y == x<!>

        <!FORBIDDEN_IDENTITY_EQUALS!>1L === 3<!>
        <!FORBIDDEN_IDENTITY_EQUALS!>x === 3<!>
        <!FORBIDDEN_IDENTITY_EQUALS!>3 === 1L<!>
        <!FORBIDDEN_IDENTITY_EQUALS!>3 === x<!>
        <!FORBIDDEN_IDENTITY_EQUALS!>y === x<!>

        yield("")
    }
}
