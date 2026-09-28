// RUN_PIPELINE_TILL: CODEGEN
class C {
    fun foo() {}
}

typealias CA = C

val cf = CA::foo

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, propertyDeclaration,
typeAliasDeclaration */
