// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

@JsExport fun test() = 1

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

fun test() = 2

// FILE: C.kt
@file:Suppress("OPT_IN_USAGE")
package c

fun test() = 3
