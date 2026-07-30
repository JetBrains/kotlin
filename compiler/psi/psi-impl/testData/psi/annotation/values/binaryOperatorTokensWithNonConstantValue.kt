// COMPILATION_ERRORS
// Operation tokens which are never compile-time constants, so they are valid only as source stubs
// FILE: Binary.kt
annotation class Binary(val value: Int)

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
