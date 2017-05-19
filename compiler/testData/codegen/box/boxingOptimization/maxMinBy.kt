// WITH_RUNTIME

fun box(): String {
    val intList = listOf(1, 2, 3)
    val longList = listOf(1L, 2L, 3L)

    val intListMin = intList.minBy { it }
    if (intListMin != 1) return "Fail intListMin=$intListMin"

    val intListMax = intList.maxBy { it }
    if (intListMax != 3) return "Fail intListMax=$intListMax"

    val longListMin = longList.minBy { it }
    if (longListMin != 1L) return "Fail longListMin=$longListMin"

    val longListMax = longList.maxBy { it }
    if (longListMax != 3L) return "Fail longListMax=$longListMax"

    return "OK"
}