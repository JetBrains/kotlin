// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -FullValueClasses
// WITH_STDLIB

@JvmInline
value class Inline(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>Any<!>()

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
