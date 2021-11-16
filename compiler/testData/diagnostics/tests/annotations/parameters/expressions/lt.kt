// FIR_IDENTICAL
// !LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean,
        val b4: Boolean,
        val b5: Boolean,
        val b6: Boolean
)

val a = 1
val b = 2

@Ann(1 < 2, 1.0 < 2.0, <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>2 < a<!>, <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>b < a<!>, 'b' < 'a', "a" < "b") class MyClass

// EXPECTED: @Ann(b1 = true, b2 = true, b3 = false, b4 = false, b5 = false, b6 = true)
