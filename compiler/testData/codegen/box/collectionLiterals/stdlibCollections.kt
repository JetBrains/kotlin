// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalStdlibApi::class)

fun Collection<*>.checkEquals(vararg es: Any?): Boolean {
    return size == es.size && zip(es).all { (a, b) -> a == b }
}

fun testList(): Boolean {
    val empty: List<Any?> = []
    val singleton: List<String> = ["single"]
    val many: List<Int> = [1, 2, 3]

    return empty.checkEquals() && singleton.checkEquals("single") && many.checkEquals(1, 2, 3)
}

fun testMutableList(): Boolean {
    val empty: MutableList<Any?> = []
    val singleton: MutableList<String> = ["single"]
    val many: MutableList<Int> = [1, 2, 3]
    return empty.apply { add(1L) }.checkEquals(1L)
            && singleton.apply { add("second") }.checkEquals("single", "second")
            && many.apply { add(4) }.checkEquals(1, 2, 3, 4)
}

fun testSet(): Boolean {
    val empty: Set<Any?> = []
    val singleton: Set<String> = ["single"]
    val many: Set<Int> = [1, 1, 2, 2, 3, 3]

    return empty.checkEquals() && singleton.checkEquals("single") && many.checkEquals(1, 2, 3)
}

fun testMutableSet(): Boolean {
    val empty: MutableSet<Any?> = []
    val singleton: MutableSet<String> = ["single"]
    val many: MutableSet<Int> = [1, 1, 2, 2, 3, 3]

    return empty.apply { add(1L); add(1L) }.checkEquals(1L)
            && singleton.apply { add("second"); add("second") }.checkEquals("single", "second")
            && many.apply { add(4); add(4) }.checkEquals(1, 2, 3, 4)
}

fun box(): String {
    return when {
        !testList() -> "Fail#List"
        !testMutableList() -> "Fail#MutableList"
        !testSet() -> "Fail#Set"
        !testMutableSet() -> "Fail#MutableSet"
        else -> "OK"
    }
}
