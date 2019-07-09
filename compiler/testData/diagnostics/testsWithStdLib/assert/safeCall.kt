// !WITH_NEW_INFERENCE
// !IGNORE_DATA_FLOW_IN_ASSERT
// SKIP_TXT
// WITH_RUNTIME

fun test1(s: String?) {
    assert(s!!.isEmpty())
    s<!NI;UNNECESSARY_SAFE_CALL!>?.<!>length
}

fun test2(s: String?) {
    assert(s!!.isEmpty())
    s<!NI;UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length
}

fun test3(s: String?) {
    assert(s!!.isEmpty())
    <!NI;DEBUG_INFO_SMARTCAST!>s<!><!OI;UNSAFE_CALL!>.<!>length
}

fun test4() {
    val s: String? = null;
    assert(s!!.isEmpty())
    s<!NI;UNNECESSARY_SAFE_CALL!>?.<!>length
}

fun test5() {
    val s: String? = null;
    assert(s!!.isEmpty())
    s<!NI;UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length
}

fun test6() {
    val s: String? = null;
    assert(s!!.isEmpty())
    <!NI;DEBUG_INFO_SMARTCAST!>s<!><!OI;UNSAFE_CALL!>.<!>length
}

