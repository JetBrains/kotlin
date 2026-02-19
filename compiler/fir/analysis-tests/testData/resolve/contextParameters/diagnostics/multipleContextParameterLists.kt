// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!MULTIPLE_CONTEXT_LISTS!>context(s: String)<!> context(i: Int)
fun foo() {}

<!MULTIPLE_CONTEXT_LISTS!>context(s: String)<!> context(i: Int)
val bar: String get() = ""

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, getter, propertyDeclaration,
propertyDeclarationWithContext, stringLiteral */
