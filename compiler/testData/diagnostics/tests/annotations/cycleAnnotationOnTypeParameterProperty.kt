// FIR_IDENTICAL
package myPack

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val number: Int)

val <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.prop<!>) T> T.prop get() = 22
