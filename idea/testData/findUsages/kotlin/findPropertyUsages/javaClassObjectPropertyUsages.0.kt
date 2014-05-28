// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: usages
package server;

class A {
    class object {
        var <caret>foo: String = "foo"
    }
}