// !LANGUAGE: -DivisionByZeroInConstantExpressions
// !DIAGNOSTICS:-DIVISION_BY_ZERO

const val a = 1 / 0.0
const val b = 1.0 / 0
const val c = 0.0 / 0
const val d = 1.0 % 0
const val e = 0.0 % 0
const val g = 0.0.rem(0)
const val h = 0.0.div(0)

const val i = 1 / 0

val nonConst1 = 1.0 / 0
val nonConst2 = 1 / 0
val nonConst3 = 1.0 % 0
val nonConst4 = 1 % 0
val nonConst6 = 1.rem(0)
val nonConst7 = 1.div(0)
