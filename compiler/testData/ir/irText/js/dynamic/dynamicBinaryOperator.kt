// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

// FIR_IDENTICAL
fun testBinaryPlus(d: dynamic) = d + 1
fun testBinaryMinus(d: dynamic) = d - 1
fun testMul(d: dynamic) = d * 2
fun testDiv(d: dynamic) = d / 2
fun testMod(d: dynamic) = d % 2
