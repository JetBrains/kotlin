// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
package a

public open class Outer() {
    open class Inner {
        fun <caret>foo() {

        }
    }
}
