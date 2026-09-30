// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class Final(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Any<!>()

abstract value class Abstract : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Any<!>()

sealed value class Sealed : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Any<!>()

value object Object : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Any<!>()

@JvmInline
value class Inline(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>Any<!>()

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration, sealed, value */
