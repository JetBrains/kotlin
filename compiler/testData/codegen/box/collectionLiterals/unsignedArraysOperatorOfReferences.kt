// LANGUAGE: +CompanionBlocks +CollectionLiterals
// WITH_STDLIB

// FILE: imported.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

import kotlin.UIntArray.*

fun testImported(): String {
    val ref: (UInt) -> UIntArray = ::of
    return if (ref(1u).contentEquals(uintArrayOf(1u))) "" else "Fail#Imported"
}

// FILE: full.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

fun box(): String {
    testImported().let { if (it.isNotEmpty()) return it }

    val ubyte: (UByteArray) -> UByteArray = UByteArray::of
    val ushort: (UShortArray) -> UShortArray = UShortArray::of
    val uint: (UIntArray) -> UIntArray = UIntArray::of
    val ulong: (ULongArray) -> ULongArray = ULongArray::of

    val ubyte0: () -> UByteArray = UByteArray::of
    val ubyte1: (UByte) -> UByteArray = UByteArray::of
    val ubyteM: (UByte, UByte) -> UByteArray = UByteArray::of
    val ushort0: () -> UShortArray = UShortArray::of
    val ushort1: (UShort) -> UShortArray = UShortArray::of
    val ushortM: (UShort, UShort) -> UShortArray = UShortArray::of
    val uint0: () -> UIntArray = UIntArray::of
    val uint1: (UInt) -> UIntArray = UIntArray::of
    val uintM: (UInt, UInt) -> UIntArray = UIntArray::of
    val ulong0: () -> ULongArray = ULongArray::of
    val ulong1: (ULong) -> ULongArray = ULongArray::of
    val ulongM: (ULong, ULong) -> ULongArray = ULongArray::of

    return when {
        !ubyte(ubyteArrayOf()).contentEquals(ubyteArrayOf()) -> "Fail#UByte0"
        !ubyte(ubyteArrayOf(1u, 2u)).contentEquals(ubyteArrayOf(1u, 2u)) -> "Fail#UByteM"
        !ushort(ushortArrayOf()).contentEquals(ushortArrayOf()) -> "Fail#UShort0"
        !ushort(ushortArrayOf(1u, 2u)).contentEquals(ushortArrayOf(1u, 2u)) -> "Fail#UShortM"
        !uint(uintArrayOf()).contentEquals(uintArrayOf()) -> "Fail#UInt0"
        !uint(uintArrayOf(1u, 2u)).contentEquals(uintArrayOf(1u, 2u)) -> "Fail#UIntM"
        !ulong(ulongArrayOf()).contentEquals(ulongArrayOf()) -> "Fail#ULong0"
        !ulong(ulongArrayOf(1uL, 2uL)).contentEquals(ulongArrayOf(1uL, 2uL)) -> "Fail#ULongM"

        !ubyte0().contentEquals(ubyteArrayOf()) -> "Fail#AdaptedUByte0"
        !ubyte1(1u).contentEquals(ubyteArrayOf(1u)) -> "Fail#AdaptedUByte1"
        !ubyteM(1u, 2u).contentEquals(ubyteArrayOf(1u, 2u)) -> "Fail#AdaptedUByteM"
        !ushort0().contentEquals(ushortArrayOf()) -> "Fail#AdaptedUShort0"
        !ushort1(1u).contentEquals(ushortArrayOf(1u)) -> "Fail#AdaptedUShort1"
        !ushortM(1u, 2u).contentEquals(ushortArrayOf(1u, 2u)) -> "Fail#AdaptedUShortM"
        !uint0().contentEquals(uintArrayOf()) -> "Fail#AdaptedUInt0"
        !uint1(1u).contentEquals(uintArrayOf(1u)) -> "Fail#AdaptedUInt1"
        !uintM(1u, 2u).contentEquals(uintArrayOf(1u, 2u)) -> "Fail#AdaptedUIntM"
        !ulong0().contentEquals(ulongArrayOf()) -> "Fail#AdaptedULong0"
        !ulong1(1uL).contentEquals(ulongArrayOf(1uL)) -> "Fail#AdaptedULong1"
        !ulongM(1uL, 2uL).contentEquals(ulongArrayOf(1uL, 2uL)) -> "Fail#AdaptedULongM"
        else -> "OK"
    }
}
