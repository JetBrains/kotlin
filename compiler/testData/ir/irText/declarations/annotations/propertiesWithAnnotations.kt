// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class TestAnn(val x: String)

@TestAnn("testVal.property")
val testVal: String = ""
