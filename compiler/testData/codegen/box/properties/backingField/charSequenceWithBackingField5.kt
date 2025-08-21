// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

open class Base {
    open val x: CharSequence = "BASE"
         // field = "BASE"
}

class Ok : Base() {
    override val x: CharSequence
        @Suppress("WRONG_MODIFIER_TARGET", "NON_FINAL_PROPERTY_WITH_EXPLICIT_BACKING_FIELD")
        internal field: String = "OK"
}

fun box(): String {
    return Ok().x
}
