// FIR_IDENTICAL
package myPack

@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

fun function(param: @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>function(42)<!>) Int) = 1
