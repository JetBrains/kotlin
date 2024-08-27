// ISSUE: KT-70813
// FIR_DUMP

fun test_Long() {
    fun Long.foo() {}

    2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
    2<!UNNECESSARY_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
}

fun test_LongResolution() {
    fun Int.foo(): Int = 1

    if (true) {
        fun Long.foo(): String = "2"

        val x: Int = 2.foo()
        val y: Int? = 2 <!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    }
}

fun test_Int() {
    fun Int.foo() {}

    2.foo()
    2<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
}

fun test_Short() {
    fun Short.foo() {}

    2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
    2<!UNNECESSARY_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
}
