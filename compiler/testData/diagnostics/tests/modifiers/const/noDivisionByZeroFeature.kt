// !LANGUAGE: -DivisionByZeroInConstantExpressions
// !DIAGNOSTICS:-DIVISION_BY_ZERO

const val a = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 / 0.0<!>
const val b = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0 / 0<!>
const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>0.0 / 0<!>

const val i = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 / 0<!>

val nonConst1 = 1.0 / 0
val nonConst2 = 1 / 0
