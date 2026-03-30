// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textBoolean: (flag: Boolean, color: String) -> Unit = { _, _ -> }

fun Wrapper(<!UNSUPPORTED!>...Text.$props(textBoolean)<!>) {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration */
