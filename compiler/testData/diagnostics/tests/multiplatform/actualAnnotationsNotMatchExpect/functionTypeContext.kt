// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class A

expect fun foo(f: context(@A String) () -> Unit)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun foo(f: context(String) () -> Unit) {}

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, expect, functionDeclaration, functionalType, typeWithContext */
