// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-65581

typealias Aliased = Int
annotation class Tag(vararg val tags: Aliased)

/* GENERATED_FIR_TAGS: annotationDeclaration, primaryConstructor, propertyDeclaration, typeAliasDeclaration, vararg */
