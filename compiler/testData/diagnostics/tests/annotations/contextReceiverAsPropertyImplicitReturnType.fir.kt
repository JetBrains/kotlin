// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(List<@Anno("context receiver type $prop") Int>)
val foo get() = this<!UNRESOLVED_LABEL!>@List<!>

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, const, getter, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, stringLiteral, thisExpression */
