// WITH_STDLIB

fun box(): String {
    val s = StringBuilder()

    val xs = StringBuilder("abcd")

    for ((index, x) in xs.withIndex()) {
        s.append("$index:$x;")
        xs.setLength(0)
    }

    val ss = s.toString()
    return if (ss == "0:a;") "OK" else "fail: '$ss'"
}