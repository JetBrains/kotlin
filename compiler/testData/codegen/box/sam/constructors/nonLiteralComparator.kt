// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    val list = mutableListOf(3, 2, 4, 8, 1, 5)
    val expected = listOf(8, 5, 4, 3, 2, 1)
    val comparatorFun: (Int, Int) -> Int = { a, b -> b - a }
    list.sortWith(Comparator(comparatorFun))
    return if (list == expected) "OK" else list.toString()
}
