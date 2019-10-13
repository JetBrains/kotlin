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
    return "OK"
}