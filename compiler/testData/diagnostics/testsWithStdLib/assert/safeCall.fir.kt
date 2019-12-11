// !WITH_NEW_INFERENCE
// !IGNORE_DATA_FLOW_IN_ASSERT
// SKIP_TXT
// WITH_RUNTIME

fun test1(s: String?) {
    assert(s!!.isEmpty())
    s?.length
}

fun test2(s: String?) {
    assert(s!!.isEmpty())
    s!!.length
}

fun test3(s: String?) {
    assert(s!!.isEmpty())
    s.length
}

fun test4() {
    val s: String? = null;
    assert(s!!.isEmpty())
    s?.length
}

fun test5() {
    val s: String? = null;
    assert(s!!.isEmpty())
    s!!.length
}

fun test6() {
    val s: String? = null;
    assert(s!!.isEmpty())
    s.length
}

