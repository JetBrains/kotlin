// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

// FIR_IDENTICAL
fun testLess(d: dynamic) = d < 2
fun testLessOrEqual(d: dynamic) = d <= 2
fun testGreater(d: dynamic) = d > 2
fun testGreaterOrEqual(d: dynamic) = d >= 2
