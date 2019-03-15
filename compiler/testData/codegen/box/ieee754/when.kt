// !LANGUAGE: -ProperIeee754Comparisons
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR

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
            else -> {}
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