// TARGET_BACKEND: JS_IR
// KT-65195, KT-87887

fun testArrayIncrementDecrement(d: dynamic) {
    val t1 = ++d["prefixIncr"]
    val t2 = --d["prefixDecr"]
    val t3 = d["postfixIncr"]++
    val t4 = d["postfixDecr"]--

    ++d["prefixIncr"]
    --d["prefixDecr"]
    d["postfixIncr"]++
    d["postfixDecr"]--
}
