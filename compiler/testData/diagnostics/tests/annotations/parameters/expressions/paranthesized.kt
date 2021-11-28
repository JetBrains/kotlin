// FIR_IDENTICAL
package test

annotation class Ann(<!MISSING_VAL_ON_ANNOTATION_PARAMETER!>i: Int<!>)

@Ann((1 + 2) * 2) class MyClass

// EXPECTED: @Ann(i = 6)
