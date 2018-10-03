fun peek() = "A"

fun box(): String {
    val x = when (val s = peek()) {
        "A" -> "OK"
        "B" -> "B"
        else -> "other $s"
    }
    return x
}
