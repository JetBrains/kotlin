// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class Test1(val x: Int, val y: Int)

class Test2(x: Int, val y: Int) {
    val x = x
}

class Test3(x: Int, val y: Int) {
    val x: Int

    init {
        this.x = x
    }
}
