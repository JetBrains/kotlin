// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// FIR_IDENTICAL
package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean
)

@Ann(!true, <!NO_VALUE_FOR_PARAMETER!>!false)<!> class MyClass

// EXPECTED: @Ann(b1 = false, b2 = true)
