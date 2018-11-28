// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// LANGUAGE_VERSION: 1.0

fun box(): String {
    val plusZero: Any = 0.0
    val minusZero: Any = -0.0
    val nullDouble: Double? = null
    if (plusZero is Double) {
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