// COMPILATION_ERRORS
// Only compile-time constants are valid annotation arguments, but all operation tokens still have to be representable in stubs
// FILE: Binary.kt
annotation class Binary(val value: Int)

// FILE: WithInfixFunction.kt
@Binary(1 shl 2)
class WithInfixFunction

// FILE: WithRange.kt
@Binary(1..2)
class WithRange

// FILE: WithElvis.kt
@Binary(null ?: 1)
class WithElvis

// FILE: WithIdentityEquality.kt
@Binary(1 === 2)
class WithIdentityEquality

// FILE: WithInOperator.kt
@Binary(1 in 2..3)
class WithInOperator

// FILE: WithNotInOperator.kt
@Binary(1 !in 2..3)
class WithNotInOperator
