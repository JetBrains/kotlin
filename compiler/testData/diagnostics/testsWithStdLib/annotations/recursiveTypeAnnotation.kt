// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-80940

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyAnnotation(val arg: @MyAnnotation String = "")

// MODULE: main(lib)
// FILE: main.kt

fun foo(arg: @MyAnnotation String) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, outProjection, primaryConstructor,
propertyDeclaration */
