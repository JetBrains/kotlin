// RUN_PIPELINE_TILL: CODEGEN

annotation class Anno(val value: String)

@Anno(constant)
const val constant = "OK"

/* GENERATED_FIR_TAGS: annotationDeclaration, const, primaryConstructor, propertyDeclaration, stringLiteral */
