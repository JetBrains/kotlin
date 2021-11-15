// WITH_STDLIB

val cs: CharSequence = "abcd"

fun box(): String {
    val s = StringBuilder()

    for ((index, x) in cs.withIndex()) {
        if (index == 0) continue
        if (index == 3) break
        s.append("$index:$x;")
    }

    val ss = s.toString()
    return if (ss == "1:b;2:c;") "OK" else "fail: '$ss'"
}