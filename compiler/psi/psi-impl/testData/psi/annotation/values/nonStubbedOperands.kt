// Not every expression is stub-based, so such operands are absent from the stub tree and have to be found via the AST
// FILE: Anno.kt
annotation class Anno(val value: Int)

// FILE: WithCast.kt
// The inner expression of the parenthesized expression is a cast
@Anno((1 as Int))
class WithCast

// FILE: WithCastOperand.kt
// The operand of the binary expression is a cast
@Anno(1 + (2 as Int))
class WithCastOperand
