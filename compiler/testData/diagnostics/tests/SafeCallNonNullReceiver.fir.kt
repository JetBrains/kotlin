// !LANGUAGE: -SafeCallsAreAlwaysNullable
// http://youtrack.jetbrains.net/issue/KT-418

fun ff() {
    val i: Int = 1
    val a: Int = <!INITIALIZER_TYPE_MISMATCH!>i<!SAFE_CALL_WILL_CHANGE_NULLABILITY!><!UNNECESSARY_SAFE_CALL!>?.<!>plus(2)<!><!>
}
