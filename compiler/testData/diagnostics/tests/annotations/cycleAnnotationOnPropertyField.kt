// FIR_IDENTICAL
package myPack

annotation class Anno(val number: String)

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
@field:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
var variableToResolve = "${42}"
    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
    get() = field + "str"
    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
    set(@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>) value) = Unit
