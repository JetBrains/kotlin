// IGNORE_BACKEND: JVM_IR
// TODO KT-36645 Don't box primitive values in string template and string concatenation in JVM_IR

// FILE: list.kt

val intList = listOf(1, 2, 3)
val longList = listOf(1L, 2L, 3L)

// FILE: box.kt

fun box(): String {
    val intListMin = intList.minBy { it } ?: -1
    if (intListMin != 1) return "Fail intListMin=$intListMin"

    val intListMax = intList.maxBy { it } ?: -1
    if (intListMax != 3) return "Fail intListMax=$intListMax"

    val longListMin = longList.minBy { it } ?: -1
    if (longListMin != 1L) return "Fail longListMin=$longListMin"

    val longListMax = longList.maxBy { it } ?: -1
    if (longListMax != 3L) return "Fail longListMax=$longListMax"

    return "OK"
}

// @BoxKt.class:
// -- no boxing
// 0 valueOf
// -- no compareTo
// 0 compareTo
// -- comparisons are properly fused with conditional jumps
// comparisons: 0 + fake inline variables: 12
// 12 ICONST_0
// 1 IF_ICMPGE
// 1 IF_ICMPLE
// 4 LCMP
// 1 IFGE
// 1 IFLE
