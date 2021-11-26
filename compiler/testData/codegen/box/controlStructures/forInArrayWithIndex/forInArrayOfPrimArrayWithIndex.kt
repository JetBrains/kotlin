// WITH_STDLIB

fun box(): String {
    // [[0], [1], [2], [3]]
    val arr = Array(4) { intArrayOf(it) }

    var s = 0
    for ((i, iarr) in arr.withIndex()) {
        s += i*iarr[0]
    }

    // 0 + 1 + 4 + 9 = 14
    return if (s != 14) "Fail: $s" else "OK"
}