// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +JvmInlineValueClasses
// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class VC(val a: Any)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration, value */
