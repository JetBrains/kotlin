// FIR_IDENTICAL
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun a() = "string"

class A {
    val b: String
    init {
        a().apply {
            b = this
        }
    }
}
