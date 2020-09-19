// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages, skipImports
// FIR_COMPARISON

package server

fun <caret>processRequest() = "foo"

