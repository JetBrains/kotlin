// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

@JsExport @JsName("fInt") fun foo(x: Int) = 1
@JsExport @JsName("fStr") fun foo(x: String) = 2
