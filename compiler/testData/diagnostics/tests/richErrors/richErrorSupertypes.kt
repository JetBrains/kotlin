// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP

error class E1
error class E2 : RichError()

error object EO1
error object EO2 : RichError()

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration */
