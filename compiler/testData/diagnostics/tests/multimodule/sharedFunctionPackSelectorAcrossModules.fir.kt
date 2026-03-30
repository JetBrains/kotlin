// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

fun Text(text: String, color: String, modifier: Int = 0) {}

fun Text(text: Int, color: String, modifier: Int = 0) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Text

fun Wrapper(...Text.$sharedProps) {
    color.length
    modifier + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral */
