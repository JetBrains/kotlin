// FIR_IDENTICAL
package myPack

annotation class Anno(val number: Int)

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>function(42)<!>)
fun function(@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>function(24)<!>) param: Int = function(0)) = 1
