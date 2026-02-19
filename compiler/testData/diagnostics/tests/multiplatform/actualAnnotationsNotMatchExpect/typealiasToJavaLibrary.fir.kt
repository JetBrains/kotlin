// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidTypeAliasToCompilerRequiredAnnotation
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Retention(AnnotationRetention.SOURCE)
expect annotation class MyDeprecatedNotMatch

@Retention(AnnotationRetention.RUNTIME)
expect annotation class MyDeprecatedMatch

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> typealias MyDeprecatedNotMatch = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_WARNING!>java.lang.Deprecated<!>

actual typealias MyDeprecatedMatch = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_WARNING!>java.lang.Deprecated<!>

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, expect, typeAliasDeclaration */
