// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class C(x: Any?) {
    val s: String?
    init {
        s = x?.toString()
    }
}
