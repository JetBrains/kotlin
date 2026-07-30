// Operation tokens which are compile-time constants, but are not covered by `simpleLiterals.kt`
// FILE: Binary.kt
annotation class Binary(val i: Int = 0, val b: Boolean = false)

// FILE: WithInfixFunction.kt
@Binary(i = 1 shl 2)
class WithInfixFunction

// FILE: WithEquality.kt
@Binary(b = 1 == 2)
class WithEquality

// FILE: WithInequality.kt
@Binary(b = 1 != 2)
class WithInequality

// FILE: WithComparison.kt
@Binary(b = 1 < 2)
class WithComparison

// FILE: WithGreaterOrEqual.kt
@Binary(b = 1 >= 2)
class WithGreaterOrEqual
