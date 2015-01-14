// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetProperty
// OPTIONS: usages
package server

class A {
    class object {
        var <caret>foo: String = "foo"
    }
}
