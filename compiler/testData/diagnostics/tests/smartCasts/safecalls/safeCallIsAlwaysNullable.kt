// LANGUAGE: +SafeCallsAreAlwaysNullable
// ISSUE: KT-46860

interface A {
    fun foo(): Int
}

fun takeInt(x: Int) {}

fun test_1(a: A) {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!>
    takeInt(<!TYPE_MISMATCH!>x<!>) // should be an error
}

fun test_2(a: A?) {
    if (a != null) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!>
        takeInt(<!TYPE_MISMATCH!>x<!>) // should be an error
    }
}
