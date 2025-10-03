// LANGUAGE: +AnnotationsInMetadata -JvmLoadAnnotationsOnAnnotationProperties
// ISSUE: KT-22463
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE

// This test checks that annotations on annotation properties are not loaded _from binaries_ if the JvmLoadAnnotationsOnAnnotationProperties
// feature is disabled. So it makes sense only when `DependencyKind.Binary` is used.
// Therefore, we mute the LL tests, and expectations for K1 and latestLV should be ignored.
// MUTE_LL_FIR: `DependencyKind.Source` is used in LL tests.

// MODULE: lib
annotation class A(
    @Deprecated("", level = DeprecationLevel.ERROR)
    val value: String = "",
)

// MODULE: test(lib)
fun test(a: A): String = a.<!DEPRECATION_ERROR!>value<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
