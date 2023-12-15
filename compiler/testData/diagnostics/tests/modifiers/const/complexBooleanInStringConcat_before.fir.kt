// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions

val nonConstBool = true
const val constBool = false

const val s1 = """ ${ true && false } """
const val s2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>""" ${ nonConstBool && false } """<!>
const val s3 = """ ${ constBool && false } """
