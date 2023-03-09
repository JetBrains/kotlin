// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class TestAnn(val x: String)

@field:TestAnn("testVal.field")
val testVal = "a val"

@field:TestAnn("testVar.field")
var testVar = "a var"
