// WITH_RUNTIME
fun test(i: UByte) {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>foo<!> = i.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUByte()<!><!>
}
