// WITH_RUNTIME
fun test(i: UShort) {
    val foo = i.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUShort()<!>
}