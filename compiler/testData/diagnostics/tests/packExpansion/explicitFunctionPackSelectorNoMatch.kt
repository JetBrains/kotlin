// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textBoolean: (flag: Boolean, color: String) -> Unit = { _, _ -> }

fun Wrapper(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Text<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>textBoolean<!>)<!>) {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration */
