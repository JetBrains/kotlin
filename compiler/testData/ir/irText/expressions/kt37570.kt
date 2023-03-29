// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57428

fun a() = "string"

class A {
    val b: String
    init {
        a().apply {
            b = this
        }
    }
}
