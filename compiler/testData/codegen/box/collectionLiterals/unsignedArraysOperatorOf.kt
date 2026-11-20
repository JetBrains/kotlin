// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB

// FILE: imported.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

import kotlin.UIntArray.*

fun testImported() {
    val _: UIntArray = of()
    val _ = of(1u, 2u)
}

// FILE: full.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

fun box(): String {
    testImported()
    if (!UByteArray.of().contentEquals(ubyteArrayOf())) return "Fail#UByte0"
    if (!UByteArray.of(1u, 2u).contentEquals(ubyteArrayOf(1u, 2u))) return "Fail#UByteM"
    if (!UShortArray.of().contentEquals(ushortArrayOf())) return "Fail#UShort0"
    if (!UShortArray.of(1u, 2u).contentEquals(ushortArrayOf(1u, 2u))) return "Fail#UShortM"
    if (!UIntArray.of().contentEquals(uintArrayOf())) return "Fail#UInt0"
    if (!UIntArray.of(1u, 2u).contentEquals(uintArrayOf(1u, 2u))) return "Fail#UIntM"
    if (!ULongArray.of().contentEquals(ulongArrayOf())) return "Fail#ULong0"
    if (!ULongArray.of(1uL, 2uL).contentEquals(ulongArrayOf(1uL, 2uL))) return "Fail#ULongM"
    return "OK"
}
