// FIR_IDENTICAL
package myPack

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val number: Int)

fun <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>function<String>()<!>) T> function() = 1
