// !LANGUAGE: +ProperIeee754Comparisons
fun box(): String {
    val nullValue: Any? = null
    val nullDouble: Double? = null
    val minusZero: Any = -0.0
    if (nullValue is Double?) {
        when (nullValue) {
            -0.0 -> {
                return "fail 1"
            }
            nullDouble -> {}
            else -> return "fail 2"
        }

        if (minusZero is Double) {
            when (nullValue) {
                minusZero -> {
                    return "fail 3"
                }
                nullDouble -> {
                }
                else -> return "fail 4"
            }
        }

    }
    return "OK"
}

// 2 areEqual \(Ljava/lang/Double;Ljava/lang/Double;\)Z
// 2 areEqual \(Ljava/lang/Double;D\)Z
// 4 areEqual
