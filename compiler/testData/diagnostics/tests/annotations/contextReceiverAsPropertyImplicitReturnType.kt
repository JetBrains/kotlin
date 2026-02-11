// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
val foo get() = this@List

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, const, getter, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, stringLiteral, thisExpression */
