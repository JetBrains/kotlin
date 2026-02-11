// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
fun foo() = this@List

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, const, functionDeclaration, functionDeclarationWithContext,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression */
