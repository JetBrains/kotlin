// LANGUAGE: +ContextParameters
// FILE: DeclarationOverloads.kt
package DeclarationOverloads

fun test() {}

context(x: Int) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun test()<!> = x

val test = 0

context(x: Int) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val test<!> get() = x
