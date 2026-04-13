// RUN_PIPELINE_TILL: BACKEND
@MustBeDocumented
annotation class DocAnn

annotation class NotDocAnn

@DocAnn class My

@NotDocAnn class Your

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */
