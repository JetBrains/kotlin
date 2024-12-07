// WITH_STDLIB

// This test demonstrates a situation when a reference for a stub local inline function in not inlined
// at the 1st phase of double-inlining. See KT-69470 for more details.
// The test itself is not muted because a special condition was added to `validateIrAfterInliningOnlyPrivateFunctions`.

// FILE: 1.kt
private fun isO(item: Char) = item == 'O'
public fun isK(item: Char) = item == 'K'

public inline fun List<Char>.filterCharsPublic(predicate: (Char) -> Boolean): List<Char> {
    val result = ArrayList<Char>(size)
    for (char in this) {
        if (predicate(char)) result.add(char)
    }
    return result
}

private inline fun List<Char>.filterCharsPrivate(predicate: (Char) -> Boolean): List<Char> {
    val result = ArrayList<Char>(size)
    for (char in this) {
        if (predicate(char)) result.add(char)
    }
    return result
}

fun test(): String {
    val chars = listOf('K', 'L', 'M', 'N', 'O')

    val o1 = chars.filterCharsPublic(::isO)
    val k1 = chars.filterCharsPublic(::isK)
    val ok1 = o1[0].toString() + k1[0].toString()

    val o2 = chars.filterCharsPrivate(::isO)
    val k2 = chars.filterCharsPrivate(::isK)
    val ok2 = o2[0].toString() + k2[0].toString()

    return if (ok1 == ok2) ok1 else "Fail: ok1=$ok1, ok2=$ok2"
}

// FILE: 2.kt
fun box(): String {
    return test()
}
