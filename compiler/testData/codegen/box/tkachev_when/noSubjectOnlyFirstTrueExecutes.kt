val i: Int = 1

fun box(): String {
    return when {
        (i is Comparable<Int>) -> "OK"
        true -> "Fail"
        else -> "Fail"
    }
}