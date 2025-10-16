// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-72863

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

<!CONTEXT_CLASS_OR_CONSTRUCTOR, CONTEXT_RECEIVERS_DEPRECATED!>context<!>(List<@Anno("context receiver type $prop") Int>)
class ClassWithImplicitConstructor

<!CONTEXT_CLASS_OR_CONSTRUCTOR!>context<!>(List<@Anno("context receiver type $prop") Int>)
class ClassWithExplicitConstructor<!CONTEXT_RECEIVERS_DEPRECATED!>()<!> {
    <!CONTEXT_CLASS_OR_CONSTRUCTOR!>constructor(i: Int) : <!NO_CONTEXT_ARGUMENT!>this<!>()<!>
}

const val prop = "str"

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, primaryConstructor, propertyDeclaration,
secondaryConstructor, stringLiteral */
