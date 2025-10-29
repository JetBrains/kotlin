// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

fun box(): String {
    val ubyte0: UByteArray = []
    val ubyte1: UByteArray = [42.toUByte()]
    val ubyteM: UByteArray = [1.toUByte(), 2.toUByte(), 3.toUByte()]

    val ushort0: UShortArray = []
    val ushort1: UShortArray = [42.toUShort()]
    val ushortM: UShortArray = [1.toUShort(), 2.toUShort(), 3.toUShort()]

    val uint0: UIntArray = []
    val uint1: UIntArray = [42u]
    val uintM: UIntArray = [1u, 2u, 3u]

    val ulong0: ULongArray = []
    val ulong1: ULongArray = [42uL]
    val ulongM: ULongArray = [1uL, 2uL, 3uL]

    return when {
        !ubyte0.contentEquals(ubyteArrayOf()) -> "Fail#UByte0"
        !ubyte1.contentEquals(ubyteArrayOf(42.toUByte())) -> "Fail#UByte1"
        !ubyteM.contentEquals(ubyteArrayOf(1.toUByte(), 2.toUByte(), 3.toUByte())) -> "Fail#UByteM"

        !ushort0.contentEquals(ushortArrayOf()) -> "Fail#UShort0"
        !ushort1.contentEquals(ushortArrayOf(42.toUShort())) -> "Fail#UShort1"
        !ushortM.contentEquals(ushortArrayOf(1.toUShort(), 2.toUShort(), 3.toUShort())) -> "Fail#UShortM"

        !uint0.contentEquals(uintArrayOf()) -> "Fail#UInt0"
        !uint1.contentEquals(uintArrayOf(42u)) -> "Fail#UInt1"
        !uintM.contentEquals(uintArrayOf(1u, 2u, 3u)) -> "Fail#UIntM"

        !ulong0.contentEquals(ulongArrayOf()) -> "Fail#ULong0"
        !ulong1.contentEquals(ulongArrayOf(42uL)) -> "Fail#ULong1"
        !ulongM.contentEquals(ulongArrayOf(1uL, 2uL, 3uL)) -> "Fail#ULongM"

        else -> "OK"
    }
}