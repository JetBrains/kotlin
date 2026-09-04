// WITH_STDLIB
fun f(b: Boolean) = sequence<Int> { yield(1); if (b) yieldAll(elements = TODO()) }.toList()

fun box(): String {
    val r = f(false)
    return if (r == listOf(1)) "OK" else "Fail: $r"
}
