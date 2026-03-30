// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

open class Props {
    val a: Int = 0
    val b: String = ""
}

fun source(...Props.$props) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.source

fun target(...source.$props) {
    a + 1
    b.length
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
