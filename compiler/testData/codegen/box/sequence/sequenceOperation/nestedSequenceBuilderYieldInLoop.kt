// WITH_STDLIB
fun box(): String {
    val r = sequence {
        for (y in sequence { yield(1); yield(2) }) yield(y * 10)
    }.toList()
    return if (r == listOf(10, 20)) "OK" else "Fail: $r"
}
