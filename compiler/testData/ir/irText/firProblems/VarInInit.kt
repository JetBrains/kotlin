// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class Some(var foo: Int) {
    init {
        if (foo < 0) {
            foo = 0
        }
    }
}
