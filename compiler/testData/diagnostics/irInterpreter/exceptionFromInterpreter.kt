// FIR_IDENTICAL
// !RENDER_IR_DIAGNOSTICS_FULL_TEXT
// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// !DIAGNOSTICS: -DIVISION_BY_ZERO
// WITH_STDLIB

const val divideByZero = <!EVALUATION_ERROR!>1 / 0<!>
const val trimMarginException = "123".<!EVALUATION_ERROR!>trimMargin(" ")<!>

annotation class A(val i: Int, val b: Int)

@A(<!EVALUATION_ERROR!>1 / 0<!>, 2)
fun foo() {}
