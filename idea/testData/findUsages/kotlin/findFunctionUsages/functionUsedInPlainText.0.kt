// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: textOccurrences
// FIR_COMPARISON

package test

class Foo {
    fun <caret>foo() {

    }
}
