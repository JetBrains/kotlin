// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: -ProperIeee754Comparisons
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6

fun greater1(a: Double, b: Double) = a > b

fun greater2(a: Double?, b: Double?) = a!! > b!!

fun greater3(a: Double?, b: Double?) = a != null && b != null && a > b

fun greater4(a: Double?, b: Double?) = if (a is Double && b is Double) a > b else null!!

fun greater5(a: Any?, b: Any?) = if (a is Double && b is Double) a > b else null!!

fun box(): String {
    if (0.0 > -0.0) return "fail 0"
    if (greater1(0.0, -0.0)) return "fail 1"
    if (greater2(0.0, -0.0)) return "fail 2"
    if (greater3(0.0, -0.0)) return "fail 3"
    if (greater4(0.0, -0.0)) return "fail 4"

    // Smart casts behavior in 1.2
    if (!greater5(0.0, -0.0)) return "fail 5"

    return "OK"
}