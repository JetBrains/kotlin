// WITH_STDLIB

fun box(): String = when {
    0 in intArrayOf(1, 2, 3) -> "fail 1"
    1 !in intArrayOf(1, 2, 3) -> "fail 2"
    else -> "OK"
}