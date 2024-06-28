// FIR_IDENTICAL
annotation class Anno(val str: String)

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"${A::class}"<!>)
class A

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>B::class.toString()<!>)
class B

const val a = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"${A::class}"<!>
const val b = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>B::class.toString()<!>
