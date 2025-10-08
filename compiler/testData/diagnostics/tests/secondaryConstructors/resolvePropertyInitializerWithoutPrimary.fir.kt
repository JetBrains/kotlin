// RUN_PIPELINE_TILL: FRONTEND
class A {
    val prop: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> ""
    constructor()
}

/* GENERATED_FIR_TAGS: classDeclaration, propertyDeclaration, secondaryConstructor, stringLiteral */
