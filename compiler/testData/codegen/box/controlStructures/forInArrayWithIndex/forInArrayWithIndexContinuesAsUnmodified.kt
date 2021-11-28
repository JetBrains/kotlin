// See https://youtrack.jetbrains.com/issue/KT-22424
// IGNORE_BACKEND: JS
// WITH_STDLIB

fun testUnoptimized(): String {
    var arr = intArrayOf(1, 2, 3, 4)
    val sb = StringBuilder()
    val ixs = arr.withIndex()
    for ((i, x) in ixs) {
        sb.append("$i:$x;")
        arr = intArrayOf(10, 20)
    }
    return sb.toString()
}

fun box(): String {
    val tn = testUnoptimized()

    var arr = intArrayOf(1, 2, 3, 4)
    val sb = StringBuilder()
    for ((i, x) in arr.withIndex()) {
        sb.append("$i:$x;")
        arr = intArrayOf(10, 20)
    }

    val s = sb.toString()
    if (s != "0:1;1:2;2:3;3:4;") return "Fail: '$s'; unoptimized: '$tn'"

    return "OK"
}