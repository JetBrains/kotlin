// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun a() = "string"

class A {
    val b: String
    init {
        a().apply {
            b = this
        }
    }
}
