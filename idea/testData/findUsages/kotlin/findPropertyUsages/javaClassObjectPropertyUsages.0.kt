// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetProperty
// OPTIONS: usages
package server

class A {
    companion object {
        var <caret>foo: String = "foo"
    }
}
