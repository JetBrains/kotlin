// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// NO_SIGNATURE_DUMP
// ^KT-57428

fun a() = "string"

class A {
    val b: String
    init {
        a().apply {
            b = this
        }
    }
}
