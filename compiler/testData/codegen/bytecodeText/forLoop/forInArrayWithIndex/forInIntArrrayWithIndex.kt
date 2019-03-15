// IGNORE_BACKEND: JVM_IR
val arr = intArrayOf(10, 20, 30, 40)

fun box(): String {
    val s = StringBuilder()
    for ((index, x) in arr.withIndex()) {
        s.append("$index:$x;")
    }
    val ss = s.toString()
    return if (ss == "0:10;1:20;2:30;3:40;") "OK" else "fail: '$ss'"
}

// 0 withIndex
// 0 iterator
// 0 hasNext
// 0 next
// 0 component1
// 0 component2
// 1 ARRAYLENGTH
