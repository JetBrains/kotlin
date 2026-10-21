@file:OptIn(ExperimentalUnsignedTypes::class)

annotation class Unsigned(
    val ub: UByte = UByte.MAX_VALUE,
    val us: UShort = UShort.MAX_VALUE,
    val ui: UInt = UInt.MAX_VALUE,
    val ul: ULong = ULong.MAX_VALUE,
    val uis: UIntArray = [1u, UInt.MAX_VALUE],
    val i: Int = 1,
)

@Unsigned(ui = UInt.MAX_VALUE - 1u, ul = ULong.MAX_VALUE - 1u, uis = [UInt.MAX_VALUE - 1u])
class Annotated
