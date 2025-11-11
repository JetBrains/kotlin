// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalCollectionLiterals::class)

fun box(): String {
    val charSequence0: Array<CharSequence> = []
    val nullableString1: Array<String?> = ["single"]
    val nullableAnyM: Array<Any?> = [null, null, null]
    val nullableInt1: Array<Int?> = [42]
    val intM: Array<Int> = [1, 2, 3]

    return when {
        !charSequence0.contentEquals(arrayOf<CharSequence>()) -> "Fail#CharSequence"
        !nullableString1.contentEquals(arrayOf<String?>("single")) -> "Fail#String?"
        !nullableAnyM.contentEquals(arrayOf<Any?>(null, null, null)) -> "Fail#Any?"
        !nullableInt1.contentEquals(arrayOf<Int?>(42)) -> "Fail#Int?"
        !intM.contentEquals(arrayOf<Int>(1, 2, 3)) -> "Fail#Int"
        else -> "OK"
    }
}
