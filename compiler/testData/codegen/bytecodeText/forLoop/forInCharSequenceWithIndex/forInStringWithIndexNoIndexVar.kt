val xs = "abcd"

fun box(): String {
    val s = StringBuilder()

    for ((_, x) in xs.withIndex()) {
        s.append("$x;")
    }

    val ss = s.toString()
    return if (ss == "a;b;c;d;") "OK" else "fail: '$ss'"
}

// 0 withIndex
// 0 iterator
// 0 hasNext
// 0 next
// 0 component1
// 0 component2
// 1 length
// 1 charAt

// The ICONST_0 is for initializing the index in the lowered for-loop.
// 1 ICONST_0
