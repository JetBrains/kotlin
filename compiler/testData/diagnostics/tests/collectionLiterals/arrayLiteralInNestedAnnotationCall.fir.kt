// LANGUAGE: +ForbidProjectionsInAnnotationProperties
// DIAGNOSTICS: -REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION

annotation class Anno1In(val x: <!PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER_ERROR!>Array<in Anno2In><!>)
annotation class Anno2In(val x: <!PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER_ERROR!>Array<in String><!>)

annotation class Anno1Out(val x: <!PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER_ERROR!>Array<out Anno2Out><!>)
annotation class Anno2Out(val x: <!PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER_ERROR!>Array<out String><!>)

@Repeatable
annotation class Anno1Inv(val x: Array<Anno2Inv>)
annotation class Anno2Inv(val x: Array<String>)

@Repeatable
annotation class Anno1Vararg(vararg val x: Anno2Inv)

@Anno1In(x = [Anno2In(x = [1])])
@Anno1Out(x = [Anno2Out(x = <!ARGUMENT_TYPE_MISMATCH!>[1]<!>)])
@Anno1Inv(x = [Anno2Inv(x = <!ARGUMENT_TYPE_MISMATCH!>[1]<!>)])
@Anno1Inv(x = arrayOf(Anno2Inv(x = <!ARGUMENT_TYPE_MISMATCH!>[1]<!>)))
@Anno1Vararg(x = [Anno2Inv(x = <!ARGUMENT_TYPE_MISMATCH!>[1]<!>)])
@Anno1Vararg(Anno2Inv(x = <!ARGUMENT_TYPE_MISMATCH!>[1]<!>))
@Anno1Vararg(x = *[Anno2Inv(x = <!ARGUMENT_TYPE_MISMATCH!>[1]<!>)])
@Anno1Vararg(x = *arrayOf(Anno2Inv(x = <!ARGUMENT_TYPE_MISMATCH!>[1]<!>)))
fun foo() {}
