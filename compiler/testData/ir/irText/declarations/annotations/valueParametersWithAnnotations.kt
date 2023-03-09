// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class TestAnn(val x: String)

fun testFun(@TestAnn("testFun.x") x: Int) {}

class TestClassConstructor1(@TestAnn("TestClassConstructor1.x")x: Int) {
    val xx = x
}
