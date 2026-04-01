// WITH_STDLIB

fun box(): String {
    if (0.0f + Float.MIN_VALUE != Float.MIN_VALUE) return "fail1"
    if ((Float.MIN_VALUE - Float.MIN_VALUE) != 0.0f) return "fail2"
    return "OK"
}
