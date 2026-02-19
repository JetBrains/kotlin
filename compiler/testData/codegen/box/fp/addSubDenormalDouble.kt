// WITH_STDLIB

fun box(): String {
    if (0.0 + Double.MIN_VALUE != Double.MIN_VALUE) return "fail1"
    if ((Double.MIN_VALUE - Double.MIN_VALUE) != 0.0) return "fail2"
    return "OK"
}
