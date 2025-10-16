// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80061
// IGNORE_K1_BECAUSE_IT_FREEZES

import kotlin.reflect.<!UNRESOLVED_IMPORT!>KFunction999999999<!> as Foo

fun main() {
    <!UNRESOLVED_REFERENCE!>Foo<!>::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration */
