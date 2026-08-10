// Negative values require a prefix expression as there are no negative literals in Kotlin
// FILE: Negative.kt
annotation class Negative(
    val i: Int,
    val l: Long,
    val b: Byte,
    val s: Short,

    val d: Double,
    val f: Float,
)

// FILE: WithNegativeLiterals.kt
@Negative(
    -1,
    -1L,
    -1,
    -1,

    -1.0,
    -1.0F,
)
class WithNegativeLiterals

// FILE: WithMinValues.kt
// The minimal values are the trickiest ones as their absolute value doesn't fit into the corresponding type
@Negative(
    Int.MIN_VALUE,
    Long.MIN_VALUE,
    Byte.MIN_VALUE,
    Short.MIN_VALUE,

    -Double.MAX_VALUE,
    -Float.MAX_VALUE,
)
class WithMinValues
