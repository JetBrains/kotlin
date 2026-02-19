// IGNORE_BACKEND: JS_IR_ES6, JS_IR
// WITH_STDLIB

fun box(): String {
    val p = 1 shl 24
    val big = p.toFloat()

    val t1 = (big + 1.0f) - big
    if (t1 != 0.0f) return "fail1"

    val t2 = big - (big - 1.0f)
    if (t2 != 1.0f) return "fail2"

    return "OK"
}
