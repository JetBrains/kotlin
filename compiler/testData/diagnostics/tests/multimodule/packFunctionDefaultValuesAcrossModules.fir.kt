// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

class TextPack(
    val color: String = "blue",
    val fontSize: Int = 12,
)

fun render(text: String = "title", ...TextPack.$props) {}

fun headline(...render.$props) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.headline

fun use() {
    headline<!NO_VALUE_FOR_PARAMETER!>()<!>
    headline(text = "title", color = "blue", fontSize = 12)
    headline(text = "title", color = "blue", fontSize = 18)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration,
stringLiteral */
