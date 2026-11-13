// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1
expect interface Foo

// MODULE: m2()()(m1)
interface I
<!ACTUAL_TYPE_ALIAS_NOT_TO_CLASS!>actual typealias Foo = I | RichError<!>

/* GENERATED_FIR_TAGS: actual, expect, interfaceDeclaration, typeAliasDeclaration */
