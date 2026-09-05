// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87020

typealias TargetAlias = AnnotationTarget

@Target(TargetAlias.FIELD)
annotation class ViaTypeAlias

class C {
    @ViaTypeAlias
    val a = 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, propertyDeclaration,
typeAliasDeclaration */
