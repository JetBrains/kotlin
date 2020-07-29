// WITH_RUNTIME
fun test(i: UByte) {
    val foo = i.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUByte()<!>
}