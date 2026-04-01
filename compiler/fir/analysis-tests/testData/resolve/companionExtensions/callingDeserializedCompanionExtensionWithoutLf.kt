// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -PRE_RELEASE_CLASS

// MODULE: m1
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: m1.kt
companion fun String.foo() {}
companion val String.bar get() = 1


// MODULE: m2(m1)
// LANGUAGE: -CompanionBlocksAndExtensions
// FILE: m2.kt

fun test() {
    String.<!UNRESOLVED_REFERENCE!>foo<!>()
    String.<!UNRESOLVED_REFERENCE!>bar<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
