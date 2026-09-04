// WITH_STDLIB
fun <F : (Int) -> Int> apply(f: F): List<Int> = sequenceOf(1, 2).map(f).toList()
fun box(): String {
    val r = apply { it + 1 }
    return if (r == listOf(2, 3)) "OK" else "Fail: $r"
}
