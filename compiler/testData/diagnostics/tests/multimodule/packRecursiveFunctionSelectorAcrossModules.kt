// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

open class Props {
    val a: Int = 0
    val b: String = ""
}

fun leaf(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Props<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

fun mid(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>leaf<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.mid

fun target(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>mid<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
