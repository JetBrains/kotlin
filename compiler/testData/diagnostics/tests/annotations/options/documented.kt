// RUN_PIPELINE_TILL: CODEGEN
@MustBeDocumented
annotation class DocAnn

annotation class NotDocAnn

@DocAnn class My

@NotDocAnn class Your

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */
