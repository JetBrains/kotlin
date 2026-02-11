// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
// ISSUE: KT-72863

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
class ClassWithImplicitConstructor

context(List<@Anno("context receiver type $prop") Int>)
class ClassWithExplicitConstructor() {
    constructor(i: Int) : this()
}

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, primaryConstructor, propertyDeclaration,
secondaryConstructor, stringLiteral */
