// TARGET_BACKEND: JS_IR
// KT-65195
// IGNORE_BACKEND_K2: JS_IR

fun testArrayIncrementDecrement(d: dynamic) {
    val t1 = ++d["prefixIncr"]
    val t2 = --d["prefixDecr"]
    val t3 = d["postfixIncr"]++
    val t4 = d["postfixDecr"]--
}
