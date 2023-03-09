// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

// FIR_IDENTICAL
fun testEqeq(d: dynamic) = d == 3
fun testExclEq(d: dynamic) = d != 3
fun testEqeqeq(d: dynamic) = d === 3
fun testExclEqeq(d: dynamic) = d !== 3
