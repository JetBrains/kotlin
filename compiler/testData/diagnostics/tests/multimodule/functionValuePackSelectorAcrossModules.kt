// RUN_PIPELINE_TILL: FRONTEND

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

fun Wrapper(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>textString<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>color<!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, propertyDeclaration */
