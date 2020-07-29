// WITH_RUNTIME
fun test(i: ULong) {
    val foo = i.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toULong()<!>
}