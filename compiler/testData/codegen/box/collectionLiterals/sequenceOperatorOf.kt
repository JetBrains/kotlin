// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB

// FILE: imported.kt
import kotlin.sequences.Sequence.*

fun testImported(x: Int?) {
    val _ = of<Int>()
    val _: Sequence<Int> = of()
    val _ = of(42)
    val _ = of(1, 2, 3)
    x?.let(::of)
}

// FILE: full.kt

fun test(x: Int?): String {
    testImported(x)
    val _ = Sequence.of<Int>()
    val _: Sequence<Int> = Sequence.of()
    val _ = Sequence.of(42)
    val _ = Sequence.of(1, 2, 3)

    x?.let(Sequence::of)
    val refEmpty: () -> Sequence<Char> = Sequence::of
    val refVararg: (Array<Char>) -> Sequence<Char> = Sequence::of
    return (arrayOf('O', 'K').let(refVararg) + refEmpty()).joinToString("")
}

fun box(): String = test(42)
