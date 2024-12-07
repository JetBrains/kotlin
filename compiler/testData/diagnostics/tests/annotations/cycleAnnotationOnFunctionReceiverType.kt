// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

package myPack

@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

fun @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.function()<!>) Int.function() = 0
