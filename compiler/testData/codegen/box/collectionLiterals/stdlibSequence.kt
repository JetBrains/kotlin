// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalStdlibApi::class)

fun Sequence<*>.checkEquals(vararg es: Any?): Boolean {
    return joinToString() == es.joinToString()
}

fun testSequence(): Boolean {
    val empty: Sequence<Nothing> = []
    val single: Sequence<Any> = ["single"]
    val many: Sequence<Long> = [1, 2, 3]

    return empty.checkEquals()
            && single.checkEquals("single")
            && many.checkEquals(1L, 2L, 3L)
}

fun box(): String {
    return when {
        !testSequence() -> "Fail"
        else -> "OK"
    }
}
