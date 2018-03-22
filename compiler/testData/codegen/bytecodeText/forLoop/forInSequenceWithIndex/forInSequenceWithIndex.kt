// WITH_RUNTIME

val xs = listOf("a", "b", "c", "d").asSequence()

fun box(): String {
    val s = StringBuilder()

    for ((index, x) in xs.withIndex()) {
        s.append("$index:$x;")
    }

    val ss = s.toString()
    return if (ss == "0:a;1:b;2:c;3:d;") "OK" else "fail: '$ss'"
}

// 0 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2