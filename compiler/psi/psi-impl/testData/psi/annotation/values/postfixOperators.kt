// COMPILATION_ERRORS
// Postfix operators are never constant expressions, but they still have to be represented in stubs
// FILE: Unary.kt
annotation class Unary(val i: Int)

// FILE: WithNotNullAssertion.kt
val nullable: Int? = null

@Unary(nullable!!)
class WithNotNullAssertion

// FILE: WithIncrement.kt
var counter: Int = 0

@Unary(counter++)
class WithIncrement
