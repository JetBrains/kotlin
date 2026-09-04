// WITH_STDLIB
fun box(): String {
    var seed: () -> Int? = { 1 }
    val s = generateSequence(seed) { it + 1 }
    seed = { 100 }
    val r = s.take(2).toList()
    return if (r == listOf(1, 2)) "OK" else "Fail: $r"
}
