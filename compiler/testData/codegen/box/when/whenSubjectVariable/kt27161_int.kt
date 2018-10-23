fun peek() = 1

fun box(): String {
    val x = when (val x = peek()) {
        1 -> "OK"
        2 -> "2"
        else -> "other $x"
    }
    return x
}
