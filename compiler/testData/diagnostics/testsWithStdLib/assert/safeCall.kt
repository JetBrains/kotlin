// !WITH_NEW_INFERENCE
// !IGNORE_DATA_FLOW_IN_ASSERT
// SKIP_TXT
// WITH_RUNTIME

fun test1(s: String?) {
    assert(s!!.isEmpty())
    s<!UNNECESSARY_SAFE_CALL{NI}!>?.<!>length
}

fun test2(s: String?) {
    assert(s!!.isEmpty())
    s<!UNNECESSARY_NOT_NULL_ASSERTION{NI}!>!!<!>.length
}

fun test3(s: String?) {
    assert(s!!.isEmpty())
    <!DEBUG_INFO_SMARTCAST{NI}!>s<!><!UNSAFE_CALL{OI}!>.<!>length
}

fun test4() {
    val s: String? = null;
    assert(s!!.isEmpty())
    s<!UNNECESSARY_SAFE_CALL{NI}!>?.<!>length
}

fun test5() {
    val s: String? = null;
    assert(s!!.isEmpty())
    s<!UNNECESSARY_NOT_NULL_ASSERTION{NI}!>!!<!>.length
}

fun test6() {
    val s: String? = null;
    assert(s!!.isEmpty())
    <!DEBUG_INFO_SMARTCAST{NI}!>s<!><!UNSAFE_CALL{OI}!>.<!>length
}

