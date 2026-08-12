// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -StrictEquals
//  ^^^ KT-88389

// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_MODIFIERS!>expect<!> <!INCOMPATIBLE_MODIFIERS!>data<!> object DataObject

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual data object DataObject

/* GENERATED_FIR_TAGS: actual, data, expect, objectDeclaration */
