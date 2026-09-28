// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

val i = 65
val s = "!"
val ui = 65u

const val char = Char(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>i<!>)
const val intEquals = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>i<!> == i
const val trimMargin = "".trimMargin(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>s<!>)
const val uIntAnd = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>ui<!> and ui

/* GENERATED_FIR_TAGS: const, integerLiteral, propertyDeclaration */
