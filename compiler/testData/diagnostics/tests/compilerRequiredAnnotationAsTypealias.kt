// RUN_PIPELINE_TILL: CODEGEN

annotation class Anno
typealias Deprecated = Anno

@Deprecated
fun foo() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, typeAliasDeclaration */
