// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetObjectDeclaration
package a

class A {
    object <caret>O {
        var foo: String = "foo"
    }
}
