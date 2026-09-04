// WITH_STDLIB
fun box(): String {
    var p: (Int) -> Boolean = { true }
    val s = sequenceOf(1, 2, 3).takeWhile(p)
    p = { false }
    val r = s.toList()
    return if (r == listOf(1, 2, 3)) "OK" else "Fail: $r"
}
