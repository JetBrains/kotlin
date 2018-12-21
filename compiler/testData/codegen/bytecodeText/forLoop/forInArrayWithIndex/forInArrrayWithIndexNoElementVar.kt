// IGNORE_BACKEND: JVM_IR
val arr = arrayOf("a", "b", "c", "d")

fun box(): String {
    val s = StringBuilder()

    for ((i, _) in arr.withIndex()) {
        s.append("$i;")
    }

    val ss = s.toString()
    return if (ss == "0;1;2;3;") "OK" else "fail: '$ss'"
}

// 0 withIndex
// 0 iterator
// 0 hasNext
// 0 next
// 0 component1
// 0 component2
// 1 ARRAYLENGTH
