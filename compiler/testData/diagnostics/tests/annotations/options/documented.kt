// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@MustBeDocumented
annotation class DocAnn

annotation class NotDocAnn

@DocAnn class My

@NotDocAnn class Your

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */
