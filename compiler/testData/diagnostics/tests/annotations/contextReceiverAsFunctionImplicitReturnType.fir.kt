// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(List<@Anno("context receiver type $prop") Int>)
fun foo() = this<!UNRESOLVED_LABEL!>@List<!>

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, const, functionDeclaration, functionDeclarationWithContext,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression */
