// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidTypeAliasToCompilerRequiredAnnotation -MultiplatformRestrictions
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
expect annotation class TypealiasToKotlinPkg

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias TypealiasToKotlinPkg = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_WARNING!>kotlin.Deprecated<!>

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, expect, typeAliasDeclaration */
