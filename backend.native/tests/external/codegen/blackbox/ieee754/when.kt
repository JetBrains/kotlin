fun box(): String {
    val plusZero: Any = 0.0
    val minusZero: Any = -0.0
    if (plusZero is Double) {
        when (plusZero) {
            -0.0 -> {
            }
            else -> return "fail 1"
        }

        if (minusZero is Double) {
            when (plusZero) {
                minusZero -> {
                }
                else -> return "fail 2"
            }
        }
    }

    return "OK"
}