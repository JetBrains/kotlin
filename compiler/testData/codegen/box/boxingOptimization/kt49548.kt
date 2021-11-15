// WITH_STDLIB

val p0 = 0..3

fun test(): List<Int> {
    val progression = if (p0.last == 3) p0 + 1 else p0
    return progression.map { it }
}

fun box(): String {
    val t = test()
    if (t != listOf(0, 1, 2, 3, 1))
        return "Failed: t=$t"
    return "OK"
}
