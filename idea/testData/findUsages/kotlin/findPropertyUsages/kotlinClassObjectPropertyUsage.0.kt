// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetProperty
// OPTIONS: usages
package server

trait Some {
    companion object {
        val <caret>XX = 1
    }
}

val a = Some.XX