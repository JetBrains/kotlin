val xs = "abcd"

fun useAny(x: Any) {}

fun box(): String {
    val s = StringBuilder()

    for ((index: Any, x) in xs.withIndex()) {
        useAny(index)
        s.append("$index:$x;")
    }

    val ss = s.toString()
    return if (ss == "0:a;1:b;2:c;3:d;") "OK" else "fail: '$ss'"
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
