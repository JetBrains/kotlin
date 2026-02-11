// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
// ISSUE: KT-72863

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(List<@Anno("context receiver type $prop") Int>)
val property: Int get() = 0

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, const, getter, integerLiteral, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, stringLiteral */
