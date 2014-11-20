// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: usages
package a

public open class Outer() {
    open class Inner {
        var <caret>foo: Int = 1
    }
}