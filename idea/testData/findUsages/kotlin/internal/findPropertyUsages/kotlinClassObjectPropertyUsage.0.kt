// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
// FIR_COMPARISON

package server

interface Some {
    companion object {
        internal const val <caret>XX = 1
    }
}

val a = Some.XX