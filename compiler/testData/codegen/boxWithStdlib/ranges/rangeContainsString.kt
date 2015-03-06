fun IntRange.contains(s: String): Boolean = true

fun box(): String {
    return if ("s" in 0..1) "OK" else "fail"
}
