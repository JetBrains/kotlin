// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages
package a

public open class Outer() {
    open class Inner {
        fun <caret>foo() {

        }
    }
}