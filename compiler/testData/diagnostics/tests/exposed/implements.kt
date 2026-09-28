// RUN_PIPELINE_TILL: CODEGEN
private interface My

// valid, it's allowed to implement worse-visible interface
class Your: My

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration */
