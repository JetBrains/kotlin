// WITH_RUNTIME

fun <T : Comparable<T>> max(list: List<T>): T {
    var max = list.get(0)
    for (i in list) {
        if (i > max) {
            max = i
        }
    }
    return max
}

fun box(): String {
    val mx = max(listOf(1, 2, 3, 4))
    if (mx == 4) {
        return "OK"
    }
    return "FAIL"
}