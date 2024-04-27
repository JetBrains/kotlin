// WITH_STDLIB
fun box(): String {
    return when (val xs = listOf(1, 2, 3); xs.first { it > 1 }) {
        in xs -> "OK"
        else -> "fail"
    }
}
