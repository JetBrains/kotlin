// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB

// FILE: imported.kt
import kotlin.Array.*

fun testImported(): String {
    val _: Array<String> = of()
    if (!of(1, 2).contentEquals(arrayOf(1, 2))) return "Fail#Imported"
    val ref: (String) -> Array<String> = ::of
    if (!ref("a").contentEquals(arrayOf("a"))) return "Fail#ImportedReference"
    return ""
}

// FILE: full.kt
fun box(): String {
    testImported().let { if (it.isNotEmpty()) return it }

    if (!Array.of("a").contentEquals(arrayOf("a"))) return "Fail#String"
    if (!Array.of<Any?>(null, 42).contentEquals(arrayOf<Any?>(null, 42))) return "Fail#Nullable"
    if (!Array.of(1, 2, 3).contentEquals(arrayOf(1, 2, 3))) return "Fail#Int"
    val numbers: Array<Number> = Array.of(3.14, 42L)
    if (!numbers.contentEquals(arrayOf<Number>(3.14, 42L))) return "Fail#Number"

    val strings: (Array<String>) -> Array<String> = Array::of
    val adaptedStrings: (String, String, String) -> Array<String> = Array::of
    val adaptedEmpty: () -> Array<Int> = Array::of

    return when {
        !strings(arrayOf("a", "b")).contentEquals(arrayOf("a", "b")) -> "Fail#Reference"
        !adaptedStrings("a", "b", "c").contentEquals(arrayOf("a", "b", "c")) -> "Fail#AdaptedReference"
        !adaptedEmpty().contentEquals(arrayOf<Int>()) -> "Fail#AdaptedEmptyReference"
        else -> "OK"
    }
}
