// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val s = StringBuilder()

    for (iv in "abcd".withIndex()) {
        val (index, x) = iv
        s.append("$index:$x;")
    }

    val ss = s.toString()
    return if (ss == "0:a;1:b;2:c;3:d;") "OK" else "fail: '$ss'"
}