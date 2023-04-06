// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

annotation class TestAnn(val x: String)

@field:TestAnn("testVal.field")
val testVal = "a val"

@field:TestAnn("testVar.field")
var testVar = "a var"
