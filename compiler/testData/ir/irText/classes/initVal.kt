// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class TestInitValFromParameter(val x: Int)

class TestInitValInClass {
    val x = 0
}

class TestInitValInInitBlock {
    val x: Int
    init {
        x = 0
    }
}
