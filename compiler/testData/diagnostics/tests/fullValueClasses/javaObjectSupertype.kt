// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class Final(val x: Int) : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Object<!>()

value object Object : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Object<!>()

@JvmInline
value class Inline(val x: Int) : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Object<!>()

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration, value */
