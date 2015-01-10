// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
package server

fun <caret>foo() {

}

fun Int.foo() {

}

fun foo(n: Int) {

}

val foo: Int
