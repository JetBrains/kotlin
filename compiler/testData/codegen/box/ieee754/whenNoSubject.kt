// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: -ProperIeee754Comparisons
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6

fun box(): String {
    val plusZero: Any = 0.0
    val minusZero: Any = -0.0
    if (plusZero is Double && minusZero is Double) {
        when {
            plusZero < minusZero -> {
                return "fail 1"
            }

            plusZero > minusZero -> {}
            else -> {
                return "fail 2"
            }
        }


        when {
            plusZero == minusZero -> {
                return "fail 3"
            }
            else -> {}
        }
    }

    return "OK"
}