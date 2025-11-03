// WITH_STDLIB

fun box(): String {
    if (1.0 / 0.0 != Double.POSITIVE_INFINITY) return "fail1"
    if (1.0 / -0.0 != Double.NEGATIVE_INFINITY) return "fail2"
    if (!((0.0 / 0.0).isNaN())) return "fail3"
    return "OK"
}
