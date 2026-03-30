// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

class TextPack(
    val color: String = "blue",
    val fontSize: Int = 12,
)

fun render(text: String = "title", <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>TextPack<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

fun headline(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>render<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.headline

fun use() {
    headline()
    headline(text = "title", color = "blue", fontSize = 12)
    headline(text = "title", color = "blue", fontSize = 18)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration,
stringLiteral */
