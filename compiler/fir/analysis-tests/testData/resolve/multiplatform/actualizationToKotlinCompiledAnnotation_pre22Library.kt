// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// ISSUE: KT-82313

// MODULE: lib
// LANGUAGE_VERSION: 2.1
// API_VERSION: 2.1
// FILE: lib.kt
annotation class Other(val x: Boolean = true)

// MODULE: common
expect annotation class Some(val x: Boolean = true)

// MODULE: platform(lib)()(common)
<!ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE!>actual<!> typealias Some = Other

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, expect, primaryConstructor, propertyDeclaration,
typeAliasDeclaration */
