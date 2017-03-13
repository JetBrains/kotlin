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
            }
            else -> return "fail 2"
        }

        if (minusZero is Double) {
            when (plusZero) {
                nullDouble -> {
                    return "fail 3"
                }
                minusZero -> {
                }
                else -> return "fail 4"
            }
        }
    }

    return "OK"
}