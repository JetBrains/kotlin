// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

annotation class TestAnn(val x: String)

@TestAnn("testVal.property")
val testVal: String = ""
