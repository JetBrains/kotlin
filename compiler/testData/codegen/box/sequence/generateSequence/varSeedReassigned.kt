// WITH_STDLIB
fun box(): String {
    var seed = 1
    val s = generateSequence(seed) { if (it < 3) it + 1 else null }
    seed = 10
    val r = s.toList()
    return if (r == listOf(1, 2, 3)) "OK" else "Fail: $r"
}
