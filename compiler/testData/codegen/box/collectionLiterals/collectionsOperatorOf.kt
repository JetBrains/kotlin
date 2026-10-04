// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB
// CHECK_BYTECODE_TEXT

// FILE: imported.kt
import kotlin.collections.List.*

fun testImported(): String {
    if (of<Int>() != listOf<Int>()) return "Fail#ImportedEmpty"
    if (of(1, 2, 3) != listOf(1, 2, 3)) return "Fail#Imported"
    val ref: (String) -> List<String> = ::of
    if (ref("a") != listOf("a")) return "Fail#ImportedReference"
    return ""
}

// FILE: full.kt
fun box(): String {
    testImported().let { if (it.isNotEmpty()) return it }

    if (List.of<String>() != listOf<String>()) return "Fail#List"
    val mutableList = MutableList.of(1, 2)
    mutableList += 3
    if (mutableList != listOf(1, 2, 3)) return "Fail#MutableList: $mutableList"
    if (Set.of("a") != setOf("a")) return "Fail#Set"
    val mutableSet = MutableSet.of(2, 1)
    mutableSet += 2
    if (mutableSet.toList() != listOf(2, 1)) return "Fail#MutableSet: $mutableSet"

    val list: (Array<String>) -> List<String> = List::of
    val emptyList: () -> List<Int> = List::of
    val adaptedMutableList: (Int, Int, Int) -> MutableList<Int> = MutableList::of
    val emptySet: () -> Set<String> = Set::of
    val singletonSet: (String) -> Set<CharSequence> = Set::of
    val adaptedMutableSet: (Char, Char, Char) -> MutableSet<Char> = MutableSet::of

    return when {
        listOf("!") != List.of("!") -> "Fail#SingletonList"
        list(arrayOf("a", "b")) != listOf("a", "b") -> "Fail#ListReference"
        emptyList() != listOf<Int>() -> "Fail#EmptyListReference"
        adaptedMutableList(1, 2, 3) != mutableListOf(1, 2, 3) -> "Fail#AdaptedMutableListReference"
        setOf("!") != singletonSet("!") -> "Fail#SingletonSet"
        emptySet() != setOf<String>() -> "Fail#EmptySetReference"
        adaptedMutableSet('a', 'b', 'a') != mutableSetOf('a', 'b') -> "Fail#AdaptedMutableSetReference"
        else -> "OK"
    }
}

// 2 MutableListStaticMembers
