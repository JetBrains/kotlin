// Unsigned values are stored as their signed counterparts in the metadata, so the value type has to be taken into account
// FILE: Unsigned.kt
annotation class Unsigned(
    val ub: UByte,
    val us: UShort,
    val ui: UInt,
    val ul: ULong,
)

// FILE: WithSmallValues.kt
@Unsigned(1u, 2u, 3u, 4uL)
class WithSmallValues

// FILE: WithMaxValues.kt
// The maximal values are the trickiest ones as they are negative in their signed representation
@Unsigned(UByte.MAX_VALUE, UShort.MAX_VALUE, UInt.MAX_VALUE, ULong.MAX_VALUE)
class WithMaxValues
