// WITH_STDLIB
fun box(): String {
    val direct = arrayOf(1, 2, 3).asSequence().map { it * 2 }.toList()
    if (direct != listOf(2, 4, 6)) return "Fail direct array: $direct"

    val array = intArrayOf(1, 2, 3)
    val viaVariable = array.asSequence().filter { it > 1 }.toList()
    if (viaVariable != listOf(2, 3)) return "Fail array variable: $viaVariable"

    val map = mapOf(1 to "a", 2 to "b")
    val entries = map.asSequence().map { it.value }.toList()
    if (entries != listOf("a", "b")) return "Fail map: $entries"

    val reused = arrayOf(1, 2).asSequence()
    if (reused.toList() != listOf(1, 2) || reused.count() != 2) return "Fail reused"
    return "OK"
}
