// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// FILE: DeclarationOverloads.kt
package DeclarationOverloads

fun test() {}

context(x: Int) fun test() = x

val test = 0

context(x: Int) val test get() = x
