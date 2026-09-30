// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class Final(val x: Int) : Any()

abstract value class Abstract : Any()

sealed value class Sealed : Any()

value object Object : Any()

@JvmInline
value class Inline(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>Any<!>()

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration, sealed, value */
