// FIR_IDENTICAL
package myPack

annotation class Anno(val number: Int)

val @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.prop<!>) Int.prop get() = 22
