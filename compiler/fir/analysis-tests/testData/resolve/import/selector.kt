// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib
// FILE: lib.kt

package lib

fun Int.extension(): Int = 2
fun topLevel(): Int = 2

// MODULE: app(lib)
// FILE: app.kt

package app

import lib[extension]

val x = 1.extension()
val y = <!UNRESOLVED_REFERENCE!>topLevel<!>()