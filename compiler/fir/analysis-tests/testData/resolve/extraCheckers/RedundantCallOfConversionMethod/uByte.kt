// WITH_STDLIB
fun test(i: UByte) {
    val <!UNUSED_VARIABLE!>foo<!> = i.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUByte()<!>
}
