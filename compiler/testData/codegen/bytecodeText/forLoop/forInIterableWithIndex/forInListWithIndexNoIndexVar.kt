val xs = listOf("a", "b", "c", "d")

fun box(): String {
    val s = StringBuilder()

    for ((_, x) in xs.withIndex()) {
        s.append("$x;")
    }

    val ss = s.toString()
    return if (ss == "a;b;c;d;") "OK" else "fail: '$ss'"
}

// 0 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2

// The 1st ICONST_0 is for initializing the list. 2nd is for initializing the index in the lowered for-loop.
// 2 ICONST_0
