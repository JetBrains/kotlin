// IGNORE_BACKEND: JVM_IR
val arr = arrayOf("a", "b", "c", "d")

fun box(): String {
    val s = StringBuilder()

    for ((_, x) in arr.withIndex()) {
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
// 1 ARRAYLENGTH
