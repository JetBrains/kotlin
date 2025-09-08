// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// SKIP_JAVAC
// FIR_IDENTICAL
// LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Test(val x: Int = 42)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration,
value */
