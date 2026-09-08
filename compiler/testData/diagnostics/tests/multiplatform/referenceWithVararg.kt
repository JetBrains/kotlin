// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
// ISSUE: KT-40674

fun foo() {
    1.let(::listOf) // Should be Ok
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
