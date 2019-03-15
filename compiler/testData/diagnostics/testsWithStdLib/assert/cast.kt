// !WITH_NEW_INFERENCE
// !IGNORE_DATA_FLOW_IN_ASSERT
// SKIP_TXT
// WITH_RUNTIME

interface A {}

class B: A {
    fun bool() = true
}

fun test1(a: A) {
    assert((a as B).bool())
    <!NI;DEBUG_INFO_SMARTCAST!>a<!>.<!OI;UNRESOLVED_REFERENCE!>bool<!>()
}

fun test2() {
    val a: A? = null;
    assert((a as B).bool())
    <!NI;DEBUG_INFO_SMARTCAST!>a<!><!NI;UNNECESSARY_SAFE_CALL!>?.<!><!OI;UNRESOLVED_REFERENCE!>bool<!>()
}
