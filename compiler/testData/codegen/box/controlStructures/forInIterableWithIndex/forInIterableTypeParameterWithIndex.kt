// WITH_STDLIB

val xs = listOf("a", "b", "c", "d")

fun <T : Iterable<*>> test(iterable: T): String {
    val s = StringBuilder()

    for ((index, x) in iterable.withIndex()) {
        s.append("$index:$x;")
    }

    return s.toString()
}

fun box(): String {
    val ss = test(xs)
    return if (ss == "0:a;1:b;2:c;3:d;") "OK" else "fail: '$ss'"
}