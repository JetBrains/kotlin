// LANGUAGE: +ContextParameters
// FILE: DeclarationOverloads.kt
package DeclarationOverloads

<!JS_NAME_CLASH!>fun test()<!> {}

context(x: Int) fun test() = x

<!JS_NAME_CLASH!>val test<!> = 0

context(x: Int) val test get() = x
