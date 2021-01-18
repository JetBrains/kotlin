// WITH_RUNTIME

fun test(s: Sequence<Int>) {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>foo<!> = s.<!USELESS_CALL_ON_NOT_NULL!>orEmpty()<!><!>
}
