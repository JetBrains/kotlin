// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib
// FILE: lib.kt
package lib

fun source(a: Int, b: String) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.source

fun target(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>source<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
