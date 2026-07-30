// COMPILATION_ERRORS
// Not every expression is stub-based, so such operands are absent from the stub tree and have to be found via the AST
// FILE: Anno.kt
annotation class Anno(val value: Int)

// FILE: WithArrayAccess.kt
val array: IntArray = intArrayOf(1)

// The operand of the prefix expression is an array access expression
@Anno(-array[0])
class WithArrayAccess

// FILE: WithSafeCall.kt
val nullable: Int? = null

// The right operand of the binary expression is a safe qualified expression
@Anno(1 + nullable?.inc())
class WithSafeCall
