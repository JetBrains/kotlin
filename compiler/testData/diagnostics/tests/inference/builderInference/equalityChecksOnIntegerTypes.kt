// !LANGUAGE: -ProperEqualityChecksInBuilderInferenceCalls
// WITH_STDLIB

fun main(x: Long, y: Int) {
    sequence {
        <!EQUALITY_NOT_APPLICABLE_WARNING("==; Long; Int")!>1L == 3<!>
        <!EQUALITY_NOT_APPLICABLE_WARNING("==; Long; Int")!>x == 3<!>
        <!EQUALITY_NOT_APPLICABLE("==; Int; Long")!>3 == 1L<!>
        <!EQUALITY_NOT_APPLICABLE("==; Int; Long")!>3 == x<!>
        <!EQUALITY_NOT_APPLICABLE("==; Int; Long")!>y == x<!>

        <!EQUALITY_NOT_APPLICABLE("===; Long; Int")!>1L === 3<!>
        <!EQUALITY_NOT_APPLICABLE("===; Long; Int")!>x === 3<!>
        <!EQUALITY_NOT_APPLICABLE("===; Int; Long")!>3 === 1L<!>
        <!EQUALITY_NOT_APPLICABLE("===; Int; Long")!>3 === x<!>
        <!EQUALITY_NOT_APPLICABLE("===; Int; Long")!>y === x<!>

        yield("")
    }
}