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

@Ann(1 > 2, 1.0 > 2.0, <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>2 > a<!>, <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>b > a<!>, 'b' > 'a', "a" > "b") class MyClass

// EXPECTED: @Ann(b1 = false, b2 = false, b3 = true, b4 = true, b5 = true, b6 = false)
