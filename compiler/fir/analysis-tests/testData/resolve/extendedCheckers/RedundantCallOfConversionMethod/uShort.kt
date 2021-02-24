// WITH_RUNTIME
fun test(i: UShort) {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>foo<!> = i.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUShort()<!><!>
}
