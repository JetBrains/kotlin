// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// FIR_IDENTICAL
package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean
)

@Ann(true && false, true && true) class MyClass

// EXPECTED: @Ann(b1 = false, b2 = true)
