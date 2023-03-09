// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

abstract class Base

object Test : Base() {
    val x = 1
    val y: Int
    init {
        y = x
    }
}
