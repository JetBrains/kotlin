// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB
// CHECK_BYTECODE_TEXT

@file:OptIn(ExperimentalUnsignedTypes::class)

fun take(vararg elements: Int): IntArray = elements
fun take(vararg elements: UInt): UIntArray = elements
fun take(vararg elements: String): Array<out String> = elements

fun box(): String {
    val ints = take(*IntArray.of(), 1, *IntArray.of(2, 3))
    if (!ints.contentEquals(intArrayOf(1, 2, 3))) return "Fail#Int: ${ints.contentToString()}"

    val uints = take(*UIntArray.of(), 1u, *UIntArray.of(2u, 3u))
    if (!uints.contentEquals(uintArrayOf(1u, 2u, 3u))) return "Fail#UInt: ${uints.contentToString()}"

    val strings = take(*Array.of(), "a", *Array.of("b", "c"))
    if (!strings.contentEquals(arrayOf("a", "b", "c"))) return "Fail#Array: ${strings.contentToString()}"
    return "OK"
}

// 0 SpreadBuilder
// 0 copyOf
