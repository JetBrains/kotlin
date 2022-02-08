// !LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean
)

@Ann(true and false, false or true, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>true xor false<!>) class MyClass

// EXPECTED: @Ann(b1 = false, b2 = true, b3 = true)
