// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
package server

fun foo() {

}

val <caret>foo: Int = 1

val Int.foo: Int = 2
// DISABLE-ERRORS
