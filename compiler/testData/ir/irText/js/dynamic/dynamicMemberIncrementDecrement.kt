// TARGET_BACKEND: JS_IR

fun testMemberIncrementDecrement(d: dynamic) {
    val t1 = ++d.prefixIncr
    val t2 = --d.prefixDecr
    val t3 = d.postfixIncr++
    val t4 = d.postfixDecr--
}

fun testSafeMemberIncrementDecrement(d: dynamic) {
    val t1 = ++d?.prefixIncr
    val t2 = --d?.prefixDecr
    val t3 = d?.postfixIncr++
    val t4 = d?.postfixDecr--
}
