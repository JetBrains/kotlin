// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +IntrinsicConstEvaluation
@Deprecated(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"Deprecated in Java".<!FUNCTION_CALL_EXPECTED!>abc<!><!>)
fun test() {}

fun String.abc() : String = this

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, stringLiteral, thisExpression */
