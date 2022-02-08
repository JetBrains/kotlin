// !LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean,
        val b4: Boolean
)

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>1 >= 2<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>1.0 >= 2.0<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>1 >= 1<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>1.0 >= 1.0<!>) class MyClass

// EXPECTED: @Ann(b1 = false, b2 = false, b3 = true, b4 = true)
