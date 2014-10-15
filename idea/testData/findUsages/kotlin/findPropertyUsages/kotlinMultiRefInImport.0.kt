// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: usages
package server

fun foo() {

}

val <caret>foo: Int = 1

val Int.foo: Int = 2