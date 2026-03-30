// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// MODULE: lib
// FILE: lib.kt
package lib

class Props {
    val color: String = ""
}

fun Text(text: String, ...Props.$props) {}

fun Text(value: Int, ...Props.$props) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Text

fun Wrapper(<!OVERLOAD_RESOLUTION_AMBIGUITY!>...Text.$props<!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>color<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral */
