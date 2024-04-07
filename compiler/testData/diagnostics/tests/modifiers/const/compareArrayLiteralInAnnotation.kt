// FIR_IDENTICAL

annotation class Anno(val i: Boolean)

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>['1'] == ['2']<!>)
class MyClass
