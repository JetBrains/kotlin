// Operation tokens which are not covered by other tests
package test

var counter: Int = 0

fun postfixDecrement(): Int = counter--

fun prefixDecrement(): Int = --counter

fun greater(a: Int, b: Int): Boolean = a > b

fun lessOrEqual(a: Int, b: Int): Boolean = a <= b

fun notIdentical(a: Any, b: Any): Boolean = a !== b

fun contains(a: Int, range: IntRange): Boolean = a in range

infix fun Int.`in`(other: Int): Int = this + other

// The referenced name is the same as the text of the `in` keyword, but the operation token is an identifier
fun backtickedInfixCall(a: Int, b: Int): Int = a `in` b
