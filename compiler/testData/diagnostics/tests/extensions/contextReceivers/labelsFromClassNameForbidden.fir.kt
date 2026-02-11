// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: -ContextParameters
// ISSUE: KT-63068
fun List<Int>.f() {
    this<!UNRESOLVED_LABEL!>@List<!>.size
}

<!UNSUPPORTED_FEATURE!>context(String)<!>
fun Int.f() {
    this<!UNRESOLVED_LABEL!>@String<!>.length
    this<!UNRESOLVED_LABEL!>@Int<!>.toDouble()
}

<!UNSUPPORTED_FEATURE!>context(String)<!>
val p: String get() = this<!UNRESOLVED_LABEL!>@String<!>

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, getter,
propertyDeclaration, propertyDeclarationWithContext, thisExpression */
