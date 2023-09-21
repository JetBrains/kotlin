// FIR_IDENTICAL
package myPack

annotation class Anno(val number: Int)

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>prop<!>)
var prop
    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>prop<!>)
    get() = 22
    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>prop<!>)
    set(@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>prop<!>) value) = Unit
