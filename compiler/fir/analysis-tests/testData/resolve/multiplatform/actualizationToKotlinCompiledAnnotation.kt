// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-82313

// MODULE: lib
annotation class Other(val x: Boolean = true)

// MODULE: common
expect annotation class Some(val x: Boolean = true)

// MODULE: platform(lib)()(common)
<!ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE!>actual<!> typealias Some = Other

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, expect, primaryConstructor, propertyDeclaration,
typeAliasDeclaration */
