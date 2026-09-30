// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: -FullValueClasses
// WITH_STDLIB

@JvmInline
value class Inline(val x: Int) : Any()

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
