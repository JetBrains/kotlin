// FIR_IDENTICAL
package myPack

annotation class Anno(val number: Int)

fun @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.function()<!>) Int.function() = 1
