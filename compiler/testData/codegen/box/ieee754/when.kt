// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: -ProperIeee754Comparisons
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6

fun box(): String {
    val plusZero: Any = 0.0
    val minusZero: Any = -0.0
    val nullDouble: Double? = null
    if (plusZero is Double) {
        // Smart casts behavior in 1.2
        when (plusZero) {
            nullDouble -> {
                return "fail 1"
            }
            -0.0 -> {
                return "fail 2"
            }
        }

        if (minusZero is Double) {
            // Smart casts behavior in 1.2
            when (plusZero) {
                nullDouble -> {
                    return "fail 3"
                }
                minusZero -> {
                    return "fail 4"
                }
                else -> {}
            }
        }
    }

    return "OK"
}