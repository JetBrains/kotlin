// FIR_IDENTICAL
annotation class Anno(val equal: Boolean)

class A
class B

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>A::class == B::class<!>)
class C

const val equal = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A::class == B::class<!>
