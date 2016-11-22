// IGNORE_BACKEND: JS
fun box(): String {
    val plusZero: Double? = 0.0
    val minusZero: Double = -0.0
    if (plusZero?.equals(minusZero) ?: null!!) {
        return "fail 1"
    }

    if (plusZero?.compareTo(minusZero) ?: null!! != 1) {
        return "fail 2"
    }

    return "OK"
}