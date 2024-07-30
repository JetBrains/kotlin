// WITH_STDLIB

fun test() {
    val foo: String? = null
    foo?.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>
}
