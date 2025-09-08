// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
private interface My

// valid, it's allowed to implement worse-visible interface
class Your: My

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration */
