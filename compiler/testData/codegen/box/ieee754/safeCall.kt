fun box(): String {
    val plusZero: Double? = 0.0
    val minusZero: Double = -0.0

    useBoxed(plusZero)

    if (plusZero?.equals(minusZero) ?: null!!) {
        return "fail 1"
    }

    if (plusZero?.compareTo(minusZero) ?: null!! != 1) {
        return "fail 2"
    }

    return "OK"
}

fun useBoxed(a: Any?) {}