// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textString: (text: String, color: String) -> Unit = ::Text

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.textString

fun Wrapper(...textString.$props) {
    text.length
    color.length
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, propertyDeclaration */
