// RUN_PIPELINE_TILL: FIR2IR
// FIR_IDENTICAL
// ISSUE: KT-80885, KT-80940

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyAnnotation(vararg val arg: @MyAnnotation String = [])

// MODULE: main(lib)
// FILE: main.kt

fun foo(arg: @MyAnnotation String) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, outProjection, primaryConstructor, vararg,
propertyDeclaration */
