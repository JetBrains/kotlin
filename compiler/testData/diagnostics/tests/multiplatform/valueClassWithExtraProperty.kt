// RUN_PIPELINE_TILL: CODEGEN
// IGNORE_FIR_DIAGNOSTICS
// LANGUAGE: -FullValueClasses +ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms
// LANGUAGE: -StrictEquals
//  ^^^ KT-88389
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect <!INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE_ERROR!>value<!> class Value(val x: Int) {
    context(x: String)
    <!CONTEXTUAL_OVERLOAD_SHADOWED!>val x: Int<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
@JvmInline
actual value class Value(val x: Int) {
    context(x: String)
    <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <!ACTUAL_MISSING!>x<!>: Int<!> get() = 1}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, getter, integerLiteral, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, value */
