// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-79327
context(<!WRONG_MODIFIER_TARGET!>private<!> _: String)
fun foo() {
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext */
