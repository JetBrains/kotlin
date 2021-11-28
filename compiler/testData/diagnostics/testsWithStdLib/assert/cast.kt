// !IGNORE_DATA_FLOW_IN_ASSERT
// SKIP_TXT
// WITH_STDLIB

interface A {}

class B: A {
    fun bool() = true
}

fun test1(a: A) {
    assert((a as B).bool())
    <!DEBUG_INFO_SMARTCAST!>a<!>.bool()
}

fun test2() {
    val a: A? = null;
    assert((a as B).bool())
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!><!DEBUG_INFO_SMARTCAST!>a<!><!UNNECESSARY_SAFE_CALL!>?.<!>bool()<!>
}
