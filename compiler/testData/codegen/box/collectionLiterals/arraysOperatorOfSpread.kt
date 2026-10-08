// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB
// CHECK_BYTECODE_TEXT

@file:OptIn(ExperimentalUnsignedTypes::class)

fun take(vararg elements: UInt): UIntArray = elements

fun box(): String {
    val uints = take(*UIntArray.of(), 1u, *UIntArray.of(2u, 3u))
    if (!uints.contentEquals(uintArrayOf(1u, 2u, 3u))) return "Fail#UInt: ${uints.contentToString()}"
    return "OK"
}

// 0 SpreadBuilder
// 0 copyOf
