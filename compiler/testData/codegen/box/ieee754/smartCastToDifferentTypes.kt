// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: -ProperIeee754Comparisons
// IGNORE_BACKEND: NATIVE
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
fun box(): String {
    val zero: Any = 0.0
    val floatZero: Any = -0.0F
    if (zero is Double && floatZero is Float) {
        if (zero == floatZero) return "fail 1"

        if (zero <= floatZero) return "fail 2"

        return "OK"
    }

    return "fail"
}
