// FIR_IDENTICAL
// !LANGUAGE: -SafeCallsAreAlwaysNullable
// http://youtrack.jetbrains.net/issue/KT-418

fun ff() {
    val i: Int = 1
    val a: Int = <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>i<!UNNECESSARY_SAFE_CALL!>?.<!>plus(2)<!>
}
