// IGNORE_BACKEND_K1: ANY
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields

// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

class Base {
    val x: CharSequence
        @Suppress("WRONG_MODIFIER_TARGET")
        internal field: String = "OK"

    val s: String get() = x
}

fun box(): String {
    return Base().s
}
