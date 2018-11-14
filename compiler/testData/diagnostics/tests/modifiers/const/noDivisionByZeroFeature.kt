// !LANGUAGE: -DivisionByZeroInConstantExpressions
// !DIAGNOSTICS:-DIVISION_BY_ZERO

const val a = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 / 0.0<!>
const val b = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0 / 0<!>
const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>0.0 / 0<!>
const val d = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0 % 0<!>
const val e = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>0.0 % 0<!>
const val f = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>0.0.<!DEPRECATION_ERROR!>mod<!>(0)<!>
const val g = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>0.0.rem(0)<!>
const val h = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>0.0.div(0)<!>

const val i = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 / 0<!>

val nonConst1 = 1.0 / 0
val nonConst2 = 1 / 0
val nonConst3 = 1.0 % 0
val nonConst4 = 1 % 0
val nonConst5 = 1.<!DEPRECATION_ERROR!>mod<!>(0)
val nonConst6 = 1.rem(0)
val nonConst7 = 1.div(0)
