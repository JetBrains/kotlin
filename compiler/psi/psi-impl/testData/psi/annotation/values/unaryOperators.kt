// FILE: Unary.kt
annotation class Unary(
    val i: Int,
    val l: Long,
    val d: Double,
    val f: Float,
    val b: Boolean,
    val ia: IntArray,
)

// FILE: WithMinus.kt
@Unary(-1, -1L, -1.0, -1.0F, !true, [-1, -2])
class WithMinus

// FILE: WithPlus.kt
@Unary(+1, +1L, +1.0, +1.0F, !false, [+1, +2])
class WithPlus

// FILE: WithNested.kt
@Unary(- -1, - -1L, - -1.0, - -1.0F, !!true, [- -1])
class WithNested

// FILE: WithParentheses.kt
@Unary(-(-1), -(-1L), -(-1.0), -(-1.0F), !(!true), [-(-1)])
class WithParentheses
