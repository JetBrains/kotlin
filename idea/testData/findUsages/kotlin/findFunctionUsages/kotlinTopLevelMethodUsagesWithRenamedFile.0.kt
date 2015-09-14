// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
@file:JvmName("RequestProcessor")
package server

fun <caret>processRequest() = "foo"

