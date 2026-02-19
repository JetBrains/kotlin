// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-72863

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
fun foo() {

}

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, const, functionDeclaration, functionDeclarationWithContext,
primaryConstructor, propertyDeclaration, stringLiteral */
