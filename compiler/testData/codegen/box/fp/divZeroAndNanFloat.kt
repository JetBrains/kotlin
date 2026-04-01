// WITH_STDLIB

fun box(): String {
    if (1.0f / 0.0f != Float.POSITIVE_INFINITY) return "fail1"
    if (1.0f / -0.0f != Float.NEGATIVE_INFINITY) return "fail2"
    if (!((0.0f / 0.0f).isNaN())) return "fail3"
    return "OK"
}
