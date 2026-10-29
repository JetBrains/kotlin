// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: -ExplicitInlineValueClassesSyntax -ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms -StrictEquals
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

// MODULE: common
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class CommonSomething(val value: Int)

// MODULE: platform()()(common)

@JvmInline
actual value class CommonSomething(val value: Int)

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, value */
