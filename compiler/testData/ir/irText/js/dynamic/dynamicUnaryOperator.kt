// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

// FIR_IDENTICAL
fun testUnaryMinus(d: dynamic) = -d
fun testUnaryPlus(d: dynamic) = +d
fun testExcl(d: dynamic) = !d
