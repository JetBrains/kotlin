// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class TestAnn(val x: Int)

class TestClass @TestAnn(1) constructor() {
    @TestAnn(2) constructor(x: Int) : this()
}
