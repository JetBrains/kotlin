// WITH_RUNTIME
fun test(i: UInt) {
    val foo = i.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUInt()<!>
}