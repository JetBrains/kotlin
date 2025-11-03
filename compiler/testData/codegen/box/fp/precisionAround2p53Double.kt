// WITH_STDLIB

fun box(): String {
    val p = 1L shl 53
    val big = p.toDouble()

    val t1 = (big + 1.0) - big
    if (t1 != 0.0) return "fail1"

    val t2 = big - (big - 1.0)
    if (t2 != 1.0) return "fail2"

    return "OK"
}
