// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: -ReadDeserializedContracts -UseCallsInPlaceEffect
// See KT-17479

class Test {
    val str: String
    init {
        run {
            <!CAPTURED_MEMBER_VAL_INITIALIZATION!>this@Test.str<!> = "A"
        }

        run {
            // Not sure do we need diagnostic also here
            this@Test.str = "B"
        }

        str = "C"
    }
}