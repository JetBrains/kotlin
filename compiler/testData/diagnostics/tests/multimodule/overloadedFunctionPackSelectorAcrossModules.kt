// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

class Props {
    val color: String = ""
}

fun Text(text: String, <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Props<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

fun Text(value: Int, <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Props<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Text

fun Wrapper(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Text<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>color<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral */
