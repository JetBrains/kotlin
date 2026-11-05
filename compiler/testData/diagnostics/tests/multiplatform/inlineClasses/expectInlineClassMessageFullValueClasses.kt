// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +FullValueClasses -ExplicitInlineValueClassesSyntax -ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms -StrictEquals
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

// MODULE: common
expect <!INLINE_CLASS_DEPRECATED!>inline<!> class CommonSomething(val value: Int)

// MODULE: platform()()(common)

@JvmInline
actual value class CommonSomething(val value: Int)

<!INLINE_CLASS_DEPRECATED!>inline<!> class PlatformSomething(val value: Int)

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, value */
