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

// The 1st ICONST_0 is for initializing the array. 2nd is for initializing the index in the lowered for-loop.
// 2 ICONST_0
