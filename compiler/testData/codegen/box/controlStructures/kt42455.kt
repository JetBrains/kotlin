// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val xs = listOf(1, 2, 3)
    return when (xs.first { it > 1 }) {
        in xs -> "OK"
        else -> "fail"
    }
}
