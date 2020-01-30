// !LANGUAGE: -ProperIeee754Comparisons
// DONT_TARGET_EXACT_BACKEND: JS_IR

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