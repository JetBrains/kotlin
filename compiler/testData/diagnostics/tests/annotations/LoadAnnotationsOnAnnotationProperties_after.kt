// FIR_IDENTICAL
// LANGUAGE: +AnnotationsInMetadata +JvmLoadAnnotationsOnAnnotationProperties
// ISSUE: KT-22463
// RUN_PIPELINE_TILL: FRONTEND

// This test checks that annotations on annotation properties are loaded _from binaries_ if the JvmLoadAnnotationsOnAnnotationProperties
// feature is enabled. It only makes sense when `DependencyKind.Binary` is used, however annotations are loaded correctly from source
// as well, so the deprecation error is reported in all variations of this test (FIR phased, latest LV, K1, LL) and we don't mute anything.

// MODULE: lib
annotation class A(
    @Deprecated("", level = DeprecationLevel.ERROR)
    val value: String = "",
)

// MODULE: test(lib)
fun test(a: A): String = a.<!DEPRECATION_ERROR!>value<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
