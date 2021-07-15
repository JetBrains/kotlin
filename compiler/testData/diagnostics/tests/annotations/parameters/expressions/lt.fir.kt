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

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>1 < 2<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>1.0 < 2.0<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>2 < a<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>b < a<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>'b' < 'a'<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"a" < "b"<!>) class MyClass

// EXPECTED: @Ann(b1 = true, b2 = true, b3 = false, b4 = false, b5 = false, b6 = true)
