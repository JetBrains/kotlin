// Not-a-number, infinities and the negative zero have no literal form
// FILE: Special.kt
annotation class Special(
    val d: Double,
    val f: Float,
)

// FILE: WithNaN.kt
@Special(Double.NaN, Float.NaN)
class WithNaN

// FILE: WithPositiveInfinity.kt
@Special(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
class WithPositiveInfinity

// FILE: WithNegativeInfinity.kt
@Special(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
class WithNegativeInfinity

// FILE: WithNegativeZero.kt
@Special(-0.0, -0.0F)
class WithNegativeZero
