// WITH_STDLIB
inline fun <T> Iterable<T>.myForEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}

inline fun myRepeat(times: Int, action: (Int) -> Unit) {
    for (index in 0 until times) {
        action(index)
    }
}


fun box(): String {
    listOf(1).myForEach { size ->
        myRepeat(size) {
            return "OK"
        }
    }
    return "Fail"
}
