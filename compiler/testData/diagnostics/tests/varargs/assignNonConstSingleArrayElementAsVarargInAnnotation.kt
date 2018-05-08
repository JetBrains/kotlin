// !LANGUAGE: -ProhibitNonConstValuesAsVarargsInAnnotations

val nonConstArray = longArrayOf(0)
fun nonConstFun(): LongArray = TODO()

fun nonConstLong(): Long = TODO()

annotation class Anno(vararg val value: Long)

@Anno(value = <!ANNOTATION_ARGUMENT_IS_NON_CONST!>nonConstArray<!>)
fun foo1() {}

@Anno(value = <!ANNOTATION_ARGUMENT_IS_NON_CONST!>nonConstFun()<!>)
fun foo2() {}

@Anno(value = <!ANNOTATION_ARGUMENT_IS_NON_CONST!>longArrayOf(<!ANNOTATION_ARGUMENT_IS_NON_CONST!>nonConstLong()<!>)<!>)
fun foo3() {}

@Anno(value = <!ANNOTATION_ARGUMENT_IS_NON_CONST!>[<!ANNOTATION_ARGUMENT_IS_NON_CONST!>nonConstLong()<!>]<!>)
fun foo4() {}

@Anno(value = *<!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION, ANNOTATION_ARGUMENT_MUST_BE_CONST!>nonConstArray<!>)
fun bar1() {}

@Anno(*<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>nonConstArray<!>)
fun bar2() {}
