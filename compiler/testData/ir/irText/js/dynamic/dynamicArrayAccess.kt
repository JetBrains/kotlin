// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testArrayAccess1(d: dynamic) = d["KEY"]

fun testArrayAccess2(d: dynamic) = d()["KEY"]

fun testArrayAccess3(d: dynamic) = d.get("KEY")
