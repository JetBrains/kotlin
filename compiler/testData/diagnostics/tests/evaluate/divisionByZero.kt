// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB

const val a = 0
val a1 = 0
val a2 = <!DIVISION_BY_ZERO!>1 / 0<!>
val a3 = 1 / a1
val a4 = 1 / a2
val a5 = 2 * (<!DIVISION_BY_ZERO!>1 / 0<!>)

val a6 = <!DIVISION_BY_ZERO!>1.div(0)<!>
val a7 = 1.div(a1)
val a8 = 1.div(a2)
val a9 = 2 * (<!DIVISION_BY_ZERO!>1.div(0)<!>)

val a10 = <!DIVISION_BY_ZERO!>1 / 0.0f<!>
val a11 = <!DIVISION_BY_ZERO!>1 / 0.0<!>
val a12 = <!DIVISION_BY_ZERO!>1L / 0<!>

val b1: Byte <!INITIALIZER_TYPE_MISMATCH!>=<!> <!DIVISION_BY_ZERO!>1 / 0<!>
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, DIVISION_BY_ZERO!>1 / 0<!>) val b2 = 1
@Ann(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>1 / a1<!>) val b3 = 1
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>1 / a<!>) val b4 = 1

annotation class Ann(val i : Int)

val div = <!DIVISION_BY_ZERO!>0.div(0)<!>
val rem = <!DIVISION_BY_ZERO!>0.rem(0)<!>
val mod = <!DIVISION_BY_ZERO!>0.mod(0)<!>
val floorDiv = <!DIVISION_BY_ZERO!>0.floorDiv(0)<!>

const val constDiv = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>0.div(0)<!>
const val constRem = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>0.rem(0)<!>
const val constMod = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>0.mod(0)<!>
const val constFloorDiv = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, DIVISION_BY_ZERO!>0.floorDiv(0)<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, const, integerLiteral, multiplicativeExpression, primaryConstructor,
propertyDeclaration */
